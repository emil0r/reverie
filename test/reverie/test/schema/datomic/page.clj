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
       tx-obj (rev/object-upgrade obj connection)
       obj-id (:db/id (rev/object-initiate obj connection))
       tx-rdata2 (rev/page-new-object! (assoc tx-rdata
                                         :data (assoc (:data tx-rdata) :object-id obj-id)))
       page (rev/page-get tx-rdata2)]
   (= obj-id
      (-> page :reverie.page/objects first :db/id))) => true)



;; (defn tempus [rdata]
;;   (rev/area a))
;; (tempus (ReverieDataDatomic. nil {} nil {:mode :public}))
