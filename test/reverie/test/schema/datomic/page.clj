(ns reverie.test.schema.datomic.page
  (:require [reverie.core :as rev]
            [reverie.schema.datomic :as _] ;; for reloads in midje
            )
  (:use midje.sweet
        [datomic.api :only [q db] :as d]
        [reverie.test.core :only [setup]])
  (:import reverie.core.ObjectSchemaDatomic reverie.core.ReverieDataDatomic))


(defn- init-data [command data tx-data]
  (let [tx-data (merge {:reverie.page/name "my test page"
                        :reverie.page/uri "my-test-page"
                        :reverie.page/template :main} tx-data)]
    
   (merge {:command command
           :parent nil
           :tx-data tx-data
           :rights :?} data)))

(fact
 "add page"
 (let [{:keys [connection]} (setup)
       request {}
       data (init-data :page-new nil nil)
       rdata (ReverieDataDatomic. connection request data)]
   (-> rdata rev/page-new! :data :page-id pos?))
 => true)

(fact
 "get page"
 (let [{:keys [connection]} (setup)
       request {}
       data (init-data :page-new nil nil)
       rdata (ReverieDataDatomic. connection request data)
       new-page-id (-> rdata rev/page-new! :db/id)]
   (= new-page-id (:db/id (rev/page-get (assoc-in rdata [:data :page-id] new-page-id)))))
 => true)

(fact
 "update page, delete page & restore page"
 (let [{:keys [connection]} (setup)
       request {}
       data (init-data :page-new nil nil)
       rdata (ReverieDataDatomic. connection request data)
       tx-rdata (rev/page-new! rdata)
       page (rev/page-get tx-rdata)
       tx-update (rev/page-update! (assoc-in tx-rdata [:data :tx-data] {:reverie.page/name "my updated page"}))
       updated-page (rev/page-get tx-rdata)
       tx-delete (rev/page-delete! tx-rdata)
       deleted-page (rev/page-get tx-rdata)
       tx-restore (rev/page-restore! tx-rdata)
       restored-page (rev/page-get tx-rdata)]
   {:updated (= (:reverie.page/name updated-page) "my updated page")
    :deleted (= (:reverie/active? deleted-page) false)
    :restored (= (:reverie/active? restored-page) true)})
 => {:updated true
     :deleted true
     :restored true})

(fact
 "add object to page"
 (let [{:keys [connection]} (setup)
       request {}
       data (init-data :page-new nil nil)
       rdata (ReverieDataDatomic. connection request data)
       tx-rdata (rev/page-new! rdata)
       obj (ObjectSchemaDatomic. :object/text {:text {:schema {:db/id #db/id [:db.part/db]
                                                         :db/ident :object.text/text
                                                         :db/valueType :db.type/string
                                                         :db/cardinality :db.cardinality/one
                                                         :db/doc "Text of the text object"
                                                         :db.install/_attribute :db.part/db}
                                                :initial "inital text"
                                                :input :text}})
       tx-obj (rev/object-upgrade! obj connection)
       obj-id (:db/id (rev/object-initiate! obj connection))
       tmp (rev/object-set! obj connection {:reverie/area :a} obj-id)
       tx-rdata2 (rev/page-new-object! (assoc tx-rdata
                                         :data (merge (:data tx-rdata) {:object-id obj-id})))
       page (rev/page-get tx-rdata2)
       object (rev/object-get obj connection obj-id)]
   (= {:object-id obj-id
       :area :a}
      {:object-id (-> page :reverie.page/objects first :db/id)
       :area (-> object :reverie/area)})) => true)


(fact
 "delete object from page"
 (let [{:keys [connection]} (setup)
       request {}
       data (init-data :page-new nil nil)
       rdata (ReverieDataDatomic. connection request data)
       tx-rdata (rev/page-new! rdata)
       obj (ObjectSchemaDatomic. :object/text {:text {:schema {:db/id #db/id [:db.part/db]
                                                         :db/ident :object.text/text
                                                         :db/valueType :db.type/string
                                                         :db/cardinality :db.cardinality/one
                                                         :db/doc "Text of the text object"
                                                         :db.install/_attribute :db.part/db}
                                                :initial "inital text"
                                                :input :text}})
       tx-obj (rev/object-upgrade! obj connection)
       obj-id (:db/id (rev/object-initiate! obj connection))
       tmp (rev/object-set! obj connection {:reverie/area :a} obj-id)
       tx-rdata2 (-> (assoc tx-rdata
                       :data (merge (:data tx-rdata) {:object-id obj-id}))
                     rev/page-new-object!
                     rev/page-delete-object!)
       object (rev/object-get obj connection obj-id)]
   (:reverie/active? object)) => false)

(fact
 "list objects of page"
 (let [{:keys [connection]} (setup)
       request {}
       data (init-data :page-new nil nil)
       rdata (ReverieDataDatomic. connection request data)
       tx-rdata (rev/page-new! rdata)
       obj (ObjectSchemaDatomic. :object/text {:text {:schema {:db/id #db/id [:db.part/db]
                                                         :db/ident :object.text/text
                                                         :db/valueType :db.type/string
                                                         :db/cardinality :db.cardinality/one
                                                         :db/doc "Text of the text object"
                                                         :db.install/_attribute :db.part/db}
                                                :initial "inital text"
                                                :input :text}})
       tx-obj (rev/object-upgrade! obj connection)
       obj-id1 (:db/id (rev/object-initiate! obj connection))
       obj-id2 (:db/id (rev/object-initiate! obj connection))
       obj-id3 (:db/id (rev/object-initiate! obj connection))
       tmp (rev/object-set! obj connection {:reverie/area :a :reverie/order 1 :text "obj-1"} obj-id1)
       tmp (rev/object-set! obj connection {:reverie/area :a :reverie/order 2 :text "obj-2"} obj-id2)
       tmp (rev/object-set! obj connection {:reverie/area :a :reverie/order 3 :text "obj-3"} obj-id3)
       tx-rdata2 (rev/page-new-object! (assoc tx-rdata
                                         :data (merge (:data tx-rdata) {:object-id obj-id1})))
       tx-rdata3 (rev/page-new-object! (assoc tx-rdata
                                         :data (merge (:data tx-rdata) {:object-id obj-id2})))
       ;; tx-rdata4 (rev/page-new-object! (assoc tx-rdata
       ;;                                   :data (merge (:data tx-rdata) {:object-id obj-id3})))
       page (rev/page-get tx-rdata2)
       objects (rev/page-objects tx-rdata)]
   (vec (map :object.text/text objects))) => ["obj-1", "obj-2"])

;; (defn tempus [rdata]
;;   (rev/area a))
;; (tempus (ReverieDataDatomic. nil {} nil {:mode :public}))
