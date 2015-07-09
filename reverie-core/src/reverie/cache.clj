(ns reverie.cache
  (:require [clojure.core.async :as async]
            [clojure.set :as set]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [digest :refer [md5]]
            [reverie.object :as object]
            [reverie.page :as page]
            [reverie.render :as render]
            [reverie.scheduler :as scheduler]
            [taoensso.timbre :as log])
  (:import [reverie CacheException RenderException]))

;; atom of fns to be skipped by the caching, requiring
;; them to be run every time a request comes in
(def skip-fns (atom {}))
;; are we caching during this rendering phase? control
;; mechanism for the skip macro
(def ^:dynamic *caching?* false)

(defmacro skip
  "Pass in a function to be skipped by the caching system. The function will be called every time for every request hitting the server. The functions must always take a request parameter"
  [function]
  (let [params (keys &env)]
    (cond

     (not (some #(= 'request %) params))
     (throw (CacheException. "reverie.cache/skip requires a request var inside the function it is called from"))

     :else
     `(if *caching?*
        (let [x# (str *ns* "/" '~function)]
          (if (nil? (get @skip-fns (str *ns* "/" '~function)))
            (swap! skip-fns assoc (keyword x#) ~function))
          (str "#reverie.cache/skip-start " x# " #reverie.cache/skip-end"))
        (~function ~'request)))))


(defn- get-skipped-rendering [rendered skips]
  (->> (str/split rendered #"\#reverie.cache/skip-start ")
       (map (fn [fragment]
              (if-let [skip (->> skips
                                 (filter #(.startsWith fragment (str % " #reverie.cache/skip-end")))
                                 first)]
                [(keyword skip) (subs fragment
                                      (+ (count skip)
                                         (count " #reverie.cache/skip-end"))
                                      (count fragment))]
                fragment)))
       flatten))

(defprotocol ICacheStore
  (read-cache [store options key]
    "Key should always be a string. Should raise exception on an empty key string or nil key. Options hold any data that is of interest and can't be held by any record implementing the protocol")
  (write-cache [store options key data])
  (delete-cache [store options key])
  (clear-cache [store]))

(defprotocol ICacheMananger
  (cache! [manager page request] [manager page rendered request])
  (evict! [manager page])
  (clear! [manager])
  (lookup [manager page request]))

(defprotocol IPruneStrategy
  (prune! [strategy cachemanager]))

(defprotocol ICacheKeyStrategy
  (get-hash-key [cache-key-strategy page request]))

(defrecord BasicHashKeyStrategy []
  ICacheKeyStrategy
  (get-hash-key [this page request]
    (let [cache-opts (merge (:cache (page/properties page))
                            (:cache (page/options page)))
          extra (merge (if (get-in cache-opts [:key :user?])
                         {:user-id (get-in request [:reverie :user :id])})
                       (if (fn? (get-in cache-opts [:key :fn]))
                         ((get-in cache-opts [:key :fn])
                          {:page page
                           :request request})))]
      (->>  {:serial (page/serial page)
             :uri (:uri request)
             :query-string (:query-string request)}
            (merge extra)
            (map str)
            sort
            str
            md5))))

(defmulti evict-cache! :type)
(defmethod evict-cache! :page-eviction [{:keys [page store internal]}]
  (doseq [k (get @internal (page/serial page))]
    (when k
      (delete-cache store nil k))))
(defmethod evict-cache! :default [_]
  nil)

(defrecord CacheManager [store c running? internal database
                         hash-key-strategy]
  component/Lifecycle
  (start [this]
    (log/info "Starting CacheMananger")
    (if internal
      this
      (let [c (async/chan)
            running? (atom true)]
        (async/thread
          (while @running?
            (let [v (async/<!! c)]
              (evict-cache! (assoc v
                              :store store
                              :internal internal
                              :database database)))))
        (assoc this
          :hash-key-strategy (or hash-key-strategy
                                 (BasicHashKeyStrategy.))
          :running? running?
          :internal (atom {})
          :c c))))
  (stop [this]
    (log/info "Stopping CacheMananger")
    (if-not internal
      this
      (do
        (reset! running? false)
        (async/close! c)
        (assoc this
          :running? nil
          :internal nil
          :c nil))))

  render/IRender
  (render [this _]
    (throw (RenderException. "[component request] not implemented for reverie.cache/CacheManager")))
  (render [this request hit]
    (if (string? hit)
      hit
      (map (fn [fragment]
             (if (keyword? fragment)
               (if-let [f (get @skip-fns fragment)]
                 (f request))
               fragment)) hit)))

  ICacheMananger
  (cache! [this page request]
    (cache! this page (render/render page request) request))
  (cache! [this page rendered request]
    (let [k (get-hash-key hash-key-strategy page request)
          serial (page/serial page)
          data (get @internal serial)
          skips (->> rendered
                     (re-seq #"\#reverie.cache/skip-start ([^ ]*) \#reverie.cache/skip-end")
                     (map last))
          cache-this (if (empty? skips)
                       rendered
                       (get-skipped-rendering rendered skips))]
      (swap! internal assoc serial (set/union data #{k}))
      (write-cache store
                   {:request request}
                   k
                   cache-this)))

  (evict! [this page]
    (evict-cache! {:type :page-eviction
                   :page page
                   :store store
                   :internal internal})
    (swap! internal dissoc (page/serial page)))

  (clear! [this]
    (clear-cache store))

  (lookup [this page request]
    (when (= (:request-method request) :get)
      (read-cache store {:request request} (get-hash-key hash-key-strategy page request)))))

(defn prune-task! [t {:keys [strategy cachemanager] :as opts}]
  (prune! strategy cachemanager))

(defn get-prune-task
  "Scheduled task for pruning the cache. Schedule is optional, default is every minute"
  [strategy cachemanager {:keys [schedule]}]
  (scheduler/get-task {:id :prune-cache
                       :desc "Prune the cache according to selected strategy"
                       :handler prune-task!
                       :schedule (or schedule
                                     "0 * * * * * *") ;; every minute
                       :opts {:strategy strategy
                              :cachemanager cachemanager}}))

(defn cachemananger [data]
  (map->CacheManager data))
