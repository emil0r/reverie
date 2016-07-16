(ns reverie.internal.memory
  "Internal storage memory implementation"
  (:require [reverie.internal :refer [IInternalStorage]]))


(defrecord MemStore [storage]
  IInternalStorage
  (read-storage [_ key]
    (get @storage key))
  (write-storage [_ key value]
    (swap! storage assoc key value))
  (delete-storage [_ key]
    (swap! storage dissoc key)))


(defn mem-store
  ([] (MemStore. (atom {})))
  ([storage] (MemStore. storage)))
