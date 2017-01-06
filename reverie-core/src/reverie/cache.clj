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
            [taoensso.timbre :as log]
            reverie.CacheException
            reverie.RenderException)
  (:import [reverie CacheException RenderException]))

;; atom of fns to be skipped by the caching, requiring
;; them to be run every time a request comes in
(defonce saved-fns (atom {}))
;; are we caching during this rendering phase? control
;; mechanism for the skip macro
(def ^:dynamic *caching?* false)

(defn get-fn [fragment]
  (if-let [f (get @saved-fns fragment)]
    f
    (let [f (resolve (symbol (.replace (str fragment) ":" "")))]
      (swap! saved-fns assoc fragment f)
      f)))

(defmacro skip
  "Pass in a function to be skipped by the caching system. The function will be called every time for every request hitting the server. The functions must always take a request parameter"
  [function]
  (let [params (keys &env)
        ns (str *ns*)
        fragment (str/replace (str (resolve function)) #"#'" "")]
    (cond

     (not (some #(= 'request %) params))
     (throw (CacheException. "reverie.cache/skip requires a request var inside the function it is called from"))

     :else
     `(if *caching?*
        (str "#reverie.cache/skip-start " ~fragment " #reverie.cache/skip-end")
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
  (evict! [manager page] [mananager page evict?-fn])
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
          meta (merge (if (get-in cache-opts [:key :user?])
                        {:user-id (get-in request [:reverie :user :id])})
                      (if (fn? (get-in cache-opts [:key :fn]))
                        ((get-in cache-opts [:key :fn])
                         {:page page
                          :request request})))]
      [;; key
       (->>  {:serial (page/serial page)
              :uri (:uri request)
              :query-string (:query-string request)}
             (merge meta)
             (map str)
             sort
             str
             md5)
       ;; meta options
       meta])))

(defmulti evict-cache! :type)
(defmethod evict-cache! :page-eviction [{:keys [page store internal evict?]}]
  (reduce
   (fn [out [k k-meta]]
     (if (nil? evict?)
       ;; if we have no meta to mach against,
       ;; we remove the page from cache
       (do
         (delete-cache store nil k)
         (conj out k))
       (if (evict? k-meta)
         (do
           (delete-cache store nil k)
           (conj out k))
         out)))
   [] (get @internal (page/serial page))))
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
  ;; TODO: add with-access here
  (render [this request hit]
    (if (string? hit)
      hit
      (map (fn [fragment]
             (if (keyword? fragment)
               (if-let [f (get-fn fragment)]
                 (f request))
               fragment)) hit)))

  ICacheMananger
  (cache! [this page request]
    (cache! this page (render/render page request) request))
  (cache! [this page rendered request]
    (let [[k k-meta] (get-hash-key hash-key-strategy page request)
          serial (page/serial page)
          data (get @internal serial)
          skips (->> rendered
                     (re-seq #"\#reverie.cache/skip-start ([^ ]*) \#reverie.cache/skip-end")
                     (map last))
          cache-this (if (empty? skips)
                       rendered
                       (get-skipped-rendering rendered skips))]
      (swap! internal assoc serial (assoc data k k-meta))
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

  (evict! [this page evict?-fn]
    (let [evicted (evict-cache! {:type :page-eviction
                                 :page page
                                 :store store
                                 :internal internal
                                 :evict? evict?-fn})
          meta (get @internal (page/serial page))]
      (swap! internal assoc (page/serial page) (reduce dissoc meta evicted))))

  (clear! [this]
    (clear-cache store))

  (lookup [this page request]
    (when (= (:request-method request) :get)
      (read-cache store {:request request} (first (get-hash-key hash-key-strategy page request))))))

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
