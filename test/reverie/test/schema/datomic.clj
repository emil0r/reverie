
(ns reverie.test.schema.datomic
  (:require [reverie.core :as rev]
            [reverie.schema.datomic :as _] ;; for reloads in midje
            )
  (:use midje.sweet
        [datomic.api :only [q db] :as d]
        [reverie.test.core :only [setup]])
  (:import reverie.schema.datomic.SchemaDatomic))


(fact
 "reverie-schema protocol/schema-correct? datomic -> correct"
 (let [d (SchemaDatomic. :object/text {:text {:schema {:db/id #db/id [:db.part/db]
                                                       :db/ident :object.text/text
                                                       :db/valueType :db.type/string
                                                       :db/cardinality :db.cardinality/one
                                                       :db/doc "Text of the text object"
                                                       :db.install/_attribute :db.part/db}
                                              :initial ""
                                              :input :text}})]
   (rev/schema-correct? d)) => true)

(fact
 "reverie-schema protocol/schema-correct? datomic -> not correct"
 (let [d (SchemaDatomic. :object/text {:text {:initial ""
                                                   :input :text}})]
   (rev/schema-correct? d)) => false)


(fact
 "reverie-schema protocol/schema-correct? datomic -> not correct"
 (let [d (SchemaDatomic. :object/text {:text {:initial ""
                                                   :input :text}})]
   (rev/schema-correct? d)) => false)


(fact
 "reverie-schema protocol/schema-initiate datomic"
 (let [d (SchemaDatomic. :object/text {:text {:schema {:db/id #db/id [:db.part/db]
                                                       :db/ident :object.text/text
                                                       :db/valueType :db.type/string
                                                       :db/cardinality :db.cardinality/one
                                                       :db/doc "Text of the text object"
                                                       :db.install/_attribute :db.part/db}
                                              :initial ""
                                              :input :text}})
       {:keys [database connection]} (setup)]
   (not (nil? (:tx-data (rev/schema-initiate d connection))))) => true)
