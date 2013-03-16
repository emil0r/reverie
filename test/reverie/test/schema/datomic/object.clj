(ns reverie.test.schema.datomic.object
  (:require [reverie.core :as rev]
            [reverie.schema.datomic :as _] ;; for reloads in midje
            )
  (:use midje.sweet
        [datomic.api :only [q db] :as d]
        [reverie.test.core :only [setup]])
  (:import reverie.core.ObjectSchemaDatomic reverie.core.ReverieDataDatomic))


(reset! rev/objects {})

(rev/defobject object/text [:areas [:a :b :c]
                            :attributes [{:text {:db/ident :object.text/text
                                                 :db/valueType :db.type/string
                                                 :db/cardinality :db.cardinality/one
                                                 :db/doc "Text of the text object"}
                                          :initial ""
                                          :input :text
                                          :name "Text"
                                          :description ""}]]
  [:get text])
(rev/run-schemas! (:connection (setup)))

(println (:object/text @rev/objects) "\n\n")
