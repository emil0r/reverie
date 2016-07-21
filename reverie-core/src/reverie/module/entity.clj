(ns reverie.module.entity
  (:require [clojure.string :as str]
            [reverie.util :as util]
            reverie.ModuleException)
  (:refer-clojure :exclude [name])
  (:import [reverie ModuleException]))


(defprotocol IModuleEntity
  (pk [entity]
    "Get the primary key for the entity (eg, id)")
  (fields [entity]
    "Fields for the entity in the database")
  (field [entity field]
    "Field for the entity")
  (field-options [entity field]
    "Options for the field")
  (field-attribs [entity field]
    "Attributes for the field")
  (field-attrib [entity field attribute] [entity field attribute default]
    "Attribute for the field with optional default")
  (field-name [entity field]
    "Field name for the field")
  (error-field-names [entity]
    "Return a hashmap for vlad error messages")
  (post-fn [entity]
    "Function data is run through after a post")
  (pre-save-fn [entity]
    "Function data is run through before saving the data")
  (display [entity]
    "What to display in admin interface for listings of this entity")
  (sections [entity]
    "Sections for the entity")
  (name [entity]
    "Name of entity")
  (slug [entity]
    "Get slug to be used as part of a URI")
  (table [entity]
    "Get table of entity")
  (publishing? [entity]
    "Does the entity support publishing?")
  (interface [entity]
    "Get the interface of the entity"))


(defrecord ModuleEntity [key options]
  IModuleEntity
  (pk [this] (or (:primary-key options) :id))
  (fields [this] (:fields options))
  (field [this field] (get-in options [:fields field]))
  (display [this] (or (get-in options [:interface :display])
                      {(pk this) {:name "Id"
                                  :sort :id}}))
  (post-fn [this] (get-in options [:post]))
  (pre-save-fn [this] (get-in options [:pre-save]))
  (field-options [this field]
    (get-in options [:fields field]))
  (field-attribs [this field]
    (let [options (get-in options [:fields field])]
      (reduce (fn [out k]
                (if (nil? out)
                  out
                  (if (k options)
                    (assoc out k (k options))
                    out)))
              {}
              [:max :min :placeholder :for])))
  (field-attrib [this field attribute]
    (get-in options [:fields field attribute]))
  (field-attrib [this field attribute default]
    (get-in options [:fields field attribute] default))
  (field-name [this field]
    (or (get-in options [:fields field :name])
        (-> field clojure.core/name str/capitalize)))
  (error-field-names [this]
    (into {}
          (map (fn [[k opt]]
                 [[k] (or (:name opt)
                          (-> k clojure.core/name str/capitalize))])
               (get-in options [:fields]))))
  (sections [this] (:sections options))
  (name [this] (or (:name options)
                   (-> key clojure.core/name str/capitalize)))
  (slug [this]
    (or (:slug options) (util/slugify key)))
  (table [this]
    (or (:table options) key))
  (publishing? [this]
    (->> options :publishing :publish? true?))
  (interface [this] (:interface options)))


(defn module-entity [[key options]]
  (map->ModuleEntity {:key key :options options }))
