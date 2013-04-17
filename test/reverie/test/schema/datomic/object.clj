(ns reverie.test.schema.datomic.object
  (:require [reverie.core :as rev]
            [reverie.schema.datomic :as _] ;; for reloads in midje
            )
  (:use midje.sweet
        [datomic.api :only [q db] :as d]
        [reverie.test.core :only [setup]]
        [ring.mock.request])
  (:import reverie.core.ObjectSchemaDatomic reverie.core.ReverieDataDatomic))


(reset! rev/objects {})

(def connection (:connection (setup)))

(rev/defobject object/text {:areas [:a :b :c]
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
                                          :description ""}]}
  [:get :post]
  [:text text :image image])
(rev/run-schemas! connection)

(fact
 "object-render"
 (let [schema (-> @rev/objects :object/text :schema)
       tx-obj (rev/object-initiate! schema connection)
       obj (rev/object-get schema connection (:db/id tx-obj))
       rdata (rev/reverie-data {:page-id 42 :connection connection
                                :request (request :get "/object-render")})
       tx-rdata (rev/page-new-object! (assoc rdata :object-id (:db/id tx-obj)))
       page (rev/page-get tx-rdata)]
   (println "asdf->" tx-obj)
   (println "\nasdf2->" (rev/object-set! schema connection (:db/id obj) {:reverie/area :a}))
   (rev/object-render schema connection (:db/id tx-obj) tx-rdata))
 => [:text "my initial text" :image "my initial image"])


(fact
 "object-copy!"
 (let [schema (-> @rev/objects :object/text :schema)
       rdata (rev/reverie-data {:page-id 42 :connection connection
                                :request (request :get "/object-render")})
       obj (-> rdata rev/page-objects first)
       obj2 (rev/object-copy! schema connection (:db/id obj))]
   (println (-> rdata rev/page-objects))
   (and
    (number? (:db/id obj))
    (number? (:db/id obj2))
    (not= (:db/id obj) (:db/id obj2))
;;    (= (dissoc obj :db/id) (dissoc obj2 :db/id))
    )
   [obj (keys obj2)])
 => true)
