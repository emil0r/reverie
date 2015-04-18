(ns reverie.cache.memory
  (:require [reverie.cache :refer [ICacheStore]]))

(defrecord MemStore [store]
  ICacheStore
  (read-cache [_ _ key]
    (get @store key))
  (write-cache [_ _ key data]
    (swap! store assoc key data))
  (delete-cache [_ _ key]
    (swap! store dissoc key)))


(defn mem-store
  ([] (mem-store (atom {})))
  ([store]
     (MemStore. store)))
