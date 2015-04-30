(ns reverie.cache
  (:require [clojure.core.async :as async]
            [clojure.set :as set]
            [com.stuartsierra.component :as component]
            [digest :refer [md5]]
            [reverie.object :as object]
            [reverie.page :as page]
            [reverie.render :as render]
            [taoensso.timbre :as log])
  (:import [reverie CacheException]))


(defn- get-hash-key [page request]
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
          md5)))

(defprotocol ICacheStore
  (read-cache [store options key]
    "Key should always be a string. Should raise exception on an empty key string or nil key. Options hold any data that is of interest and can't be held by any record implementing the protocol")
  (write-cache [store options key data])
  (delete-cache [store options key])
  (clear-cache [store]))

(defmulti evict-cache! :type)
(defmethod evict-cache! :page-eviction [{:keys [page store internal]}]
  (doseq [k (get @internal (page/serial page))]
    (when k
      (delete-cache store nil k))))
(defmethod evict-cache! :default [_]
  nil)

(defprotocol ICacheMananger
  (cache! [manager page request] [manager page rendered request])
  (evict! [manager page])
  (clear! [manager])
  (prune! [mananger])
  (lookup [manager page request]))

(defrecord CacheManager [store c running? internal database
                         max-stored]
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

  ICacheMananger
  (cache! [this page request]
    (cache! this page (render/render page request) request))
  (cache! [this page rendered request]
    (let [k (get-hash-key page request)
          serial (page/serial page)
          data (get @internal serial)]
      (swap! internal assoc serial (set/union data #{k}))
      (write-cache store
                   {:request request}
                   k
                   rendered)))

  (evict! [this page]
    (evict-cache! {:type :page-eviction
                   :page page
                   :store store
                   :internal internal}))

  (clear! [this]
    (clear-cache store))

  (lookup [this page request]
    (when (= (:request-method request) :get)
      (read-cache store {:request request} (get-hash-key page request)))))


(defn cachemananger [data]
  (map->CacheManager data))
