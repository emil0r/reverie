(ns reverie.test.schema.datomic.plugin
  (:require [reverie.core :as rev]
            [reverie.schema.datomic :as _] ;; for reloads in midje
            )
  (:use midje.sweet
        [datomic.api :only [q db] :as d]
        [reverie.test.core :only [setup]])
  (:import reverie.core.PluginDatomic))

(reset! rev/plugins {})

(def connection (:connection (setup)))

(rev/defplugin organizations {:schema [{:db/ident :organization/name
                                        :db/valueType :db.type/string
                                        :db/cardinality :db.cardinality/one
                                        :db/doc "Name of the organization"}
                                       {:db/ident :organization/hq
                                        :db/valueType :db.type/string
                                        :db/cardinality :db.cardinality/one
                                        :db/doc "Headquarter of the organization"}
                                       {:db/ident :organization/persons
                                        :db/valueType :db.type/ref
                                        :db/cardinality :db.cardinality/many
                                        :db/doc "People belonging to the organization"}
                                       {:db/ident :person/name
                                        :db/valueType :db.type/string
                                        :db/cardinality :db.cardinality/one
                                        :db/doc ""}
                                       {:db/ident :person/age
                                        :db/valueType :db.type/long
                                        :db/cardinality :db.cardinality/one
                                        :db/doc ""}
                                       {:db/ident :person/job
                                        :db/valueType :db.type/string
                                        :db/cardinality :db.cardinality/one
                                        :db/doc ""}]
                              :active? true})


(fact
 "plugin-correct?"
 (let [p (rev/get-plugin :organizations)]
   (rev/plugin-correct? p))
 => true)

(fact
 "plugin-upgrade?"
 (let [p (rev/get-plugin :organizations)]
   (rev/plugin-upgrade? p connection))
 => true)

(fact
 "plugin-upgrade!"
 (let [p (rev/get-plugin :organizations)]
   (rev/plugin-upgrade! p connection))
 => true)
