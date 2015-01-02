(ns reverie.module
  (:refer-clojure :exclude [list])
  (:import [reverie ModuleException]))

(defprotocol ModuleProtocol
  (list [module request offset limit])
  (get-filters [module])
  (get-removals [module])
  (related [module])
  (add! [module entity data])
  (save! [module entity data])
  (update! [module entity data])
  (delete! [module entity data]))


(defprotocol ModuleEntityProtocol
  (id [entity] [entity key])
  (fields [entity])
  (data [entity]))



(defrecord ModuleOptions [offset limit filters removals])

(defrecord Module [database options
                   list-fn
                   add-fn save-fn update-fn delete-fn]
  ModuleProtocol
  (list [this request offset limit] (list-fn database request offset limit))
  (add! [this entity data] (add-fn database entity data))
  (save! [this entity data] (save-fn database entity data))
  (update! [this entity data] (update-fn database entity data))
  (delete! [this entity data] (delete-fn database entity data)))



(defrecord ModuleEntity [data fields id-key]
  ModuleEntityProtocol
  (id [this] (or (get data id-key)
                 (get data :id)
                 (let [ids (filter #(re-find #"id" (name %)) (keys data))]
                   (cond
                    (empty? ids) (throw (ModuleException. "More than one key found for entity [ModuleEntity/id]"))
                    (= (count ids) 1) (get data (first ids))
                    :else (throw (ModuleException. "No key found for entity [ModuleEntity/id]"))))))
  (id [this key] (get data key))
  (fields [this] fields)
  (data [this] data))
