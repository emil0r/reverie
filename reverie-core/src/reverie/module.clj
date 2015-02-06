(ns reverie.module
  (:refer-clojure :exclude [list])
  (:import [reverie ModuleException]))

(defprotocol ModuleProtocol
  (interface? [entity]
    "Should this be automatically interfaced?")
  (entities [module]
    "Entities of the module"))


(defprotocol ModuleEntityProtocol
  (list [entity] [entity params offset limit]
    "List the fields in an entity in the admin interface")
  (pk [entity]
    "Get the primary key for the entity (eg, id)")
  (fields [entity]
    "Fields for the entity in the database")
  (data [entity]
    "Data for the entity")
  (get-filters [module]
    "Get possible filters")
  (related [entity]
    "Related entities and how they relate"))



(defrecord ModuleOptions [offset limit filters])

(defrecord Module [database entities entities-order]
  ModuleProtocol)



(defrecord ModuleEntity [data fields id-key]
  ModuleEntityProtocol
  (pk [this] (or (get data id-key)
                 (get data :id)
                 (let [ids (filter #(re-find #"id" (name %)) (keys data))]
                   (cond
                    ( ids) (throw (ModuleException. "More than one key found for entity [ModuleEntity/id]"))
                    (= (count ids) 1) (get data (first ids))
                    :else (throw (ModuleException. "No key found for entity [ModuleEntity/id]"))))))
  (fields [this] fields)
  (data [this] data))


(defn module-entity [[field options]]
  )

(defn module [name entities options]
  (map->Module {:name name :entities entities :options options}))
