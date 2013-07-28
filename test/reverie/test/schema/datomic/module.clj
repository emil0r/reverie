(ns reverie.test.schema.datomic.module
  (:require [reverie.core :as rev]
            [reverie.schema.datomic :as _] ;; for reloads in midje
            )
  (:use midje.sweet
        [datomic.api :only [q db] :as d]
        [reverie.test.core :only [setup]])
  (:import reverie.core.ModuleDatomic))

(reset! rev/modules {})

(def connection (:connection (setup)))

(rev/defmodule organizations {:schema [{:db/ident :organization/name
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
 "module-correct?"
 (let [p (rev/get-module :organizations)]
   (rev/module-correct? p))
 => true)

(fact
 "module-upgrade?"
 (let [p (rev/get-module :organizations)]
   (rev/module-upgrade? p connection))
 => true)

(fact
 "module-upgrade!"
 (let [p (rev/get-module :organizations)]
   (rev/module-upgrade! p connection)
   (rev/defmodule organizations {:schema [{:db/ident :organization/name
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
                                           :db/doc ""}
                                          {:db/ident :person/years-at-company
                                           :db/valueType :db.type/long
                                           :db/cardinality :db.cardinality/one
                                           :db/doc ""}]
                                 :active? true})
   (let [p (rev/get-module :organizations)]
     (rev/module-upgrade! p connection))
   [(number? (ffirst (q '[:find ?c :where [?c :db/ident :person/age]] (db connection))))
    (number? (ffirst (q '[:find ?c :where [?c :db/ident :person/years-at-company]] (db connection))))])
 => [true true])
