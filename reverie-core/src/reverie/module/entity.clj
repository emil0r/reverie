(ns reverie.module.entity
  (:require [clojure.string :as str]
            [reverie.util :as util])
  (:refer-clojure :exclude [name])
  (:import [reverie ModuleException]))


(defprotocol IModuleEntity
  (pk [entity]
    "Get the primary key for the entity (eg, id)")
  (fields [entity]
    "Fields for the entity in the database")
  (sections [entity]
    "Sections for the entity")
  (name [entity]
    "Name of entity")
  (slug [entity]
    "Get slug to be used as part of a URI")
  (table [entity]
    "Get table of entity"))


(defrecord ModuleEntity [key options]
  IModuleEntity
  (pk [this] (or (:primary-key options) :id))
  (fields [this] (:fields options))
  (sections [this] (:sections options))
  (name [this] (or (:name options)
                   (-> key clojure.core/name str/capitalize)))
  (slug [this]
    (or (:slug options) (util/slugify key))))


(defn module-entity [[key options]]
  (map->ModuleEntity {:key key :options options }))
