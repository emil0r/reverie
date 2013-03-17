(ns reverie.test.schema.datomic.object
  (:require [reverie.core :as rev]
            [reverie.schema.datomic :as _] ;; for reloads in midje
            )
  (:use midje.sweet
        [datomic.api :only [q db] :as d]
        [reverie.test.core :only [setup]])
  (:import reverie.core.ObjectSchemaDatomic reverie.core.ReverieDataDatomic))


(reset! rev/objects {})

(def connection (:connection (setup)))

(rev/defobject object/text [:areas [:a :b :c]
                            :attributes [{:text {:db/ident :object.text/text
                                                 :db/valueType :db.type/string
                                                 :db/cardinality :db.cardinality/one
                                                 :db/doc "Text of the text object"}
                                          :initial "my initial text"
                                          :input :text
                                          :name "Text"
                                          :description ""}
                                         {:image {:db/ident :object.text/image
                                                  :db/valueType :db.type/string
                                                  :db/cardinality :db.cardinality/one
                                                  :db/doc "Image of the text object"}
                                          :initial "my initial image"
                                          :input :text
                                          :name "Image"
                                          :description ""}]]
  [:get :post]
  [:text text :image image])
(rev/run-schemas! connection)

(fact
 "object-render"
 (let [request {:uri "/object-render"}
       schema (-> @rev/objects :object/text :schema)
       tx-obj (rev/object-initiate! schema connection)
       rdata (rev/reverie-data {:page-id 42 :connection connection :request {:request-method :get}})
       tx-rdata (rev/page-new-object! (assoc rdata :object-id (:db/id tx-obj)))
       page (rev/page-get tx-rdata)]
   (rev/object-render schema connection (:db/id tx-obj) tx-rdata))
 => [:text "my initial text" :image "my initial image"])
