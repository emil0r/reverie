(ns reverie.module
  (:refer-clojure :exclude [list])
  (:import [reverie ModuleException]))

(defprotocol ModuleProtocol
  (list [module] [module entity params offset limit])
  (get-filters [module])
  (get-removals [module])
  (entities [module])
  (related [module]))


(defprotocol ModuleEntityProtocol
  (id [entity] [entity key])
  (fields [entity])
  (data [entity]))



(defrecord ModuleOptions [offset limit filters removals])

(defrecord Module [database options entities entities-order list-fn]
  ModuleProtocol
  (list [this] (sort entities))
  (list [this entity params offset limit] (list-fn database params offset limit)))



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
