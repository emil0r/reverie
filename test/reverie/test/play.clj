(ns reverie.test.play
  (:require [reverie.core :as rev]
            [reverie.schema.datomic :as _])
  (:use [datomic.api :only [q db] :as d])
  (:import reverie.core.ObjectSchemaDatomic reverie.core.ReverieDataDatomic))





;; (reset! rev/objects {})

;; (def db-uri-mem "datomic:mem://reverie.play")

;; (defn setup []
;;   (d/delete-database db-uri-mem)
;;   (let [database (d/create-database db-uri-mem)
;;         connection (d/connect db-uri-mem)]
;;     {:database database
;;      :connection connection}))

;; (def connection (:connection (setup)))

;; @(d/transact connection [{:db/id (d/tempid :db.part/db)
;;                           :db/ident :db.part/foo
;;                           :db.install/_partition :db.part/db}
;;                          {:db/id (d/tempid :db.part/db)
;;                           :db/ident :db.part/bar
;;                           :db.install/_partition :db.part/db}])

;; @(d/transact connection [{:db/id (d/tempid :db.part/db)
;;                           :db/ident :asdf
;;                           :db/valueType :db.type/string
;;                           :db/cardinality :db.cardinality/one
;;                           :db/doc "asdf"
;;                           :db/unique :db.unique/value
;;                           :db.install/_attribute :db.part/db}])

;; @(d/transact connection [{:db/id #db/id [:db.part/foo -1]
;;                           :asdf "nisse/foo"}
;;                          ])

;; @(d/transact connection [{:db/id (d/tempid :bar)
;;                           :asdf "nisse/foo"}
;;                          ])


;; (defn setup []
;;   (d/delete-database db-uri-mem)
;;   (let [database (d/create-database db-uri-mem)
;;         connection (d/connect db-uri-mem)
;;         schema (read-string (slurp "schema/datomic.schema"))]
;;     @(d/transact connection schema)
;;     {:database database
;;      :connection connection}))

;; (def connection (:connection (setup)))

;; (rev/defobject object/text [:areas [:a :b :c]
;;                             :attributes [{:text {:db/ident :object.text/text
;;                                                  :db/valueType :db.type/string
;;                                                  :db/cardinality :db.cardinality/one
;;                                                  :db/doc "Text of the text object"}
;;                                           :initial "my initial text"
;;                                           :input :text
;;                                           :name "Text"
;;                                           :description ""}
;;                                          {:image {:db/ident :object.text/image
;;                                                   :db/valueType :db.type/string
;;                                                   :db/cardinality :db.cardinality/one
;;                                                   :db/doc "Image of the text object"}
;;                                           :initial "my initial image"
;;                                           :input :text
;;                                           :name "Image"
;;                                           :description ""}]]
;;   [:get :post]
;;   [:text text :image image])
;; (rev/run-schemas! connection)

;; @(d/transact connection [{:db/id #db/id[:db.part/db],
;;                           :db.install/_attribute :db.part/db,
;;                           :db/ident :object.text/text,
;;                           :db/valueType :db.type/string,
;;                           :db/cardinality :db.cardinality/one,
;;                           :db/doc "Text of the text object"}
;;                          {:db/id #db/id[:db.part/db],
;;                           :db.install/_attribute :db.part/db,
;;                           :db/ident :object.text/image,
;;                           :db/valueType :db.type/string,
;;                           :db/cardinality :db.cardinality/one,
;;                           :db/doc "Image of the text object"}])


;; (let [request {:uri "/object-render"}
;;       schema (-> @rev/objects :object/text :schema)
;;       tx-obj (rev/object-initiate! schema connection)
;;       rdata (rev/reverie-data {:page-id 42 :connection connection :request {:request-method :get}})
;;       object-id (:db/id tx-obj)
;;       tx-rdata (rev/page-new-object! (assoc rdata :object-id (:db/id tx-obj)))
;;       page (rev/page-get tx-rdata)]
;;   (println
;;    "object-id ->" object-id
;;    "\nobject keys ->" (keys (rev/object-get schema connection object-id))
;;    "\n\n--\n"
;;    (rev/object-render schema connection (:db/id tx-obj) tx-rdata)))



;; (let [tx @(d/transact connection [{
;;                                    :object.text/image "my image"
;;                                    :object.text/text "my text"
;;                                    :db/id #db/id[:db.part/user -1]
;;                                    :reverie/object :object/text
;;                                    :reverie/active? true}])
;;       id (-> tx :tempids vals last)
;;       obj (d/entity (db connection) id)]
;;   (println id (:db/id obj))
;;   (clojure.pprint/pprint (into {} obj)))

;;(println (map #(:db/ident (d/entity (db connection) (first %))) (q
;'[:find ?c :where [?c :db/ident]] (db connection))))

