(ns reverie.test.schema.datomic
  (:require [reverie.core :as rev]
            ;;[reverie.schema.datomic :as _] ;; for reloads in midje
            )
  (:use midje.sweet
        ;;[reverie.test.core :only [setup]]
        ))


;; (fact
;;  "reverie-schema protocol/object-correct? datomic -> correct"
;;  (let [d (ObjectSchemaDatomic. :object/text {:object.text/text {:schema {:db/id #db/id [:db.part/db]
;;                                                                    :db/ident :object.text/text
;;                                                                    :db/valueType :db.type/string
;;                                                                    :db/cardinality :db.cardinality/one
;;                                                                    :db/doc "Text of the text object"
;;                                                                    :db.install/_attribute :db.part/db}
;;                                                           :initial ""
;;                                                           :input :text
;;                                                           :name "Text"
;;                                                           :description ""}})]
;;    (rev/object-correct? d)) => true)

;; (fact
;;  "reverie-schema protocol/object-correct? datomic -> not correct"
;;  (let [d (ObjectSchemaDatomic. :object/text {:text {:initial ""
;;                                               :input :text}})]
;;    (rev/object-correct? d)) => false)


;; (fact
;;  "reverie-schema protocol/object-correct? datomic -> not correct"
;;  (let [d (ObjectSchemaDatomic. :object/text {:text {:initial ""
;;                                                    :input :text}})]
;;    (rev/object-correct? d)) => false)

;; (fact
;;  "reverie-schema protocol/object-upgrade? datomic"
;;  (let [d (ObjectSchemaDatomic. :object/text {:text {:schema {:db/id #db/id [:db.part/db]
;;                                                        :db/ident :object.text/text
;;                                                        :db/valueType :db.type/string
;;                                                        :db/cardinality :db.cardinality/one
;;                                                        :db/doc "Text of the text object"
;;                                                        :db.install/_attribute :db.part/db}
;;                                               :initial "object-upgrade?"
;;                                               :input :text}})
;;        {:keys [database connection]} (setup)]
;;    (rev/object-upgrade? d connection)) => true)

;; (fact
;;  "reverie-schema protocol/object-upgrade datomic"
;;  (let [d (ObjectSchemaDatomic. :object/text {:text {:schema {:db/id #db/id [:db.part/db]
;;                                                        :db/ident :object.text/text
;;                                                        :db/valueType :db.type/string
;;                                                        :db/cardinality :db.cardinality/one
;;                                                        :db/doc "Text of the text object"
;;                                                        :db.install/_attribute :db.part/db}
;;                                               :initial "object-upgrade"
;;                                               :input :text}})
;;        {:keys [database connection]} (setup)]
;;    (nil? (rev/object-upgrade! d connection))) => false)


;; (fact
;;  "reverie-schema protocol/object-initiate"
;;  (let [d (ObjectSchemaDatomic. :object/text {:text {:schema {:db/id #db/id [:db.part/db]
;;                                                        :db/ident :object.text/text
;;                                                        :db/valueType :db.type/string
;;                                                        :db/cardinality :db.cardinality/one
;;                                                        :db/doc "Text of the text object"
;;                                                        :db.install/_attribute :db.part/db}
;;                                               :initial "initiate"
;;                                               :input :text}})
;;        {:keys [database connection]} (setup)
;;        tx (rev/object-upgrade! d connection)]
;;    (nil? (rev/object-initiate! d connection))) => false)

;; (fact
;;  "reverie-schema protocol/object-initiate datomic"
;;  (let [d (ObjectSchemaDatomic. :object/text {:text {:schema {:db/id #db/id [:db.part/db]
;;                                                        :db/ident :object.text/text
;;                                                        :db/valueType :db.type/string
;;                                                        :db/cardinality :db.cardinality/one
;;                                                        :db/doc "Text of the text object"
;;                                                        :db.install/_attribute :db.part/db}
;;                                               :initial "set"
;;                                               :input :text}})
;;        {:keys [database connection]} (setup)]
;;    (rev/object-upgrade! d connection)
;;    (nil? (:db/id (rev/object-initiate! d connection)))) => false)


;; (fact
;;  "reverie-schema protocol/object-set datomic"
;;  (let [d (ObjectSchemaDatomic. :object/text {:text {:schema {:db/id #db/id [:db.part/db]
;;                                                        :db/ident :object.text/text
;;                                                        :db/valueType :db.type/string
;;                                                        :db/cardinality :db.cardinality/one
;;                                                        :db/doc "Text of the text object"
;;                                                        :db.install/_attribute :db.part/db}
;;                                               :initial "set with id"
;;                                               :input :text}})
;;        {:keys [database connection]} (setup)
;;        tx (rev/object-upgrade! d connection)
;;        id (:db/id (rev/object-initiate! d connection))]
;;    (rev/object-set! d connection id {:text "my text"})
;;    (= id (:db/id (rev/object-set! d connection id {:text "my text 2"})))) => true)


;; (fact
;;  "reverie-schema protocol/object-synchronize datomic"
;;  (let [d (ObjectSchemaDatomic. :object/text {:text {:schema {:db/id #db/id [:db.part/db]
;;                                                        :db/ident :object.text/text
;;                                                        :db/valueType :db.type/string
;;                                                        :db/cardinality :db.cardinality/one
;;                                                        :db/doc "Text of the text object"
;;                                                        :db.install/_attribute :db.part/db}
;;                                               :initial "set with id"
;;                                               :input :text}})
;;        {:keys [database connection]} (setup)
;;        tx (rev/object-upgrade! d connection)
;;        tx1 (rev/object-initiate! d connection)
;;        tx2 (rev/object-initiate! d connection)
;;        tx3 (rev/object-initiate! d connection)
;;        d2 (ObjectSchemaDatomic. :object/text {:text {:schema {:db/id #db/id [:db.part/db]
;;                                                         :db/ident :object.text/text
;;                                                         :db/valueType :db.type/string
;;                                                         :db/cardinality :db.cardinality/one
;;                                                         :db/doc "Text of the text object"
;;                                                         :db.install/_attribute :db.part/db}
;;                                                :initial "set with id"
;;                                                :input :text}
;;                                         :image {:schema {:db/id #db/id [:db.part/db]
;;                                                          :db/ident :object.text/image
;;                                                          :db/valueType :db.type/string
;;                                                          :db/cardinality :db.cardinality/one
;;                                                          :db/doc "Image of the text object"
;;                                                          :db.install/_attribute :db.part/db}
;;                                                 :initial ""
;;                                                 :input :text}})]
;;    (rev/object-upgrade! d2 connection)
;;    (nil? (rev/object-synchronize! d2 connection))) => false)



;; (fact
;;  "reverie-schema protocol/object-get datomic"
;;  (let [d (ObjectSchemaDatomic. :object/text {:text {:schema {:db/id #db/id [:db.part/db]
;;                                                        :db/ident :object.text/text
;;                                                        :db/valueType :db.type/string
;;                                                        :db/cardinality :db.cardinality/one
;;                                                        :db/doc "Text of the text object"
;;                                                        :db.install/_attribute :db.part/db}
;;                                               :initial "set with id"
;;                                               :input :text}})
;;        {:keys [database connection]} (setup)
;;        tx (rev/object-upgrade! d connection)
;;        id (:db/id (rev/object-initiate! d connection))
;;        tx2 (rev/object-set! d connection id {:text "my text"})
;;        obj1 (rev/object-get d connection id)]
;;    (:object.text/text obj1)) => "my text")


;; (fact
;;  "reverie-schema protocol/object-transform datomic"
;;  (let [d (ObjectSchemaDatomic. :object/text {:text {:schema {:db/id #db/id [:db.part/db]
;;                                                        :db/ident :object.text/text
;;                                                        :db/valueType :db.type/string
;;                                                        :db/cardinality :db.cardinality/one
;;                                                        :db/doc "Text of the text object"
;;                                                        :db.install/_attribute :db.part/db}
;;                                                     :initial "set with id"
;;                                                     :input :text}
;;                                              :image {:schema {:db/id #db/id [:db.part/db]
;;                                                               :db/ident :object.text/image
;;                                                               :db/valueType :db.type/string
;;                                                               :db/cardinality :db.cardinality/one
;;                                                               :db/doc "My image"
;;                                                               :db.install/_attribute :db.part/db}
;;                                                      :initial "my image"
;;                                                      :input :image}})
;;        {:keys [database connection]} (setup)
;;        tx (rev/object-upgrade! d connection)
;;        id (:db/id (rev/object-initiate! d connection))
;;        tx2 (rev/object-set! d connection id {:text "my text" :image "my image"})
;;        obj1 (rev/object-get d connection id)]
;;    [(->> obj1 (rev/object-attr-transform d) :text)
;;     (->> obj1 (rev/object-attr-transform d) :image)]) => ["my text"
;;                                                           "my image"])
