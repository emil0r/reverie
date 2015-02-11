(ns reverie.module
  (:require [reverie.render :as render]
            [reverie.security :refer [with-access]])
  (:refer-clojure :exclude [list])
  (:import [reverie ModuleException RenderException]))

(defprotocol IModule
  (interface? [entity]
    "Should this be automatically interfaced?")
  (entities [module]
    "Entities of the module"))


(defprotocol IModuleEntity
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

(defrecord Module [database entities options]
  IModule
  render/IRender
  (render [this request]
    (with-access
      (get-in request [:reverie :user]) (:required-roles options)
      {:status 200
       :body "hi from module?!"
       :headers {}}))
  (render [this _ _]
    (throw (RenderException. "[component request sub-component] not implemented for reverie.module/Module"))))



(defrecord ModuleEntity [data fields id-key]
  IModuleEntity
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
