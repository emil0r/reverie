(ns reverie.test.schema.datomic.object
  (:require [reverie.core :as rev]
            ;;[reverie.schema.datomic :as _] ;; for reloads in midje
            )
  (:use midje.sweet
        ;;[reverie.test.core :only [setup]]
        [ring.mock.request]))


;; (reset! rev/objects {})

;; (def connection (:connection (setup)))

;; (rev/defobject object/text {:areas [:a :b :c]
;;                             :attributes {:text {:initial "my initial text"
;;                                                 :input :text
;;                                                 :name "Text"
;;                                                 :description ""}
;;                                          :image {:initial "my initial image"
;;                                                  :input :text
;;                                                  :name "Image"
;;                                                  :description ""}}}
;;   [:get :post]
;;   [:text text :image image])
;; (rev/run-schemas! connection)
;; (def rdata (rev/reverie-data {:connection connection
;;                               :request (request :get "/object-render")
;;                               :tx-data {:reverie.page/uri "/object-render"
;;                                         :reverie.page/name "page"
;;                                         :reverie.page/template :main}}))
;; (def page1 (rev/page-new! rdata))
;; (def rdata (merge rdata {:page-id (:page-id page)
;;                          :area :a}))
;; (def page2 (rev/page-new! (rev/reverie-data {:connection connection
;;                                              :tx-data {:reverie.page/uri "/page2"
;;                                                        :reverie.page/name "page 2"
;;                                                        :reverie.page/template :main}})))


;; (fact
;;  "object-render"
;;  (let [schema (-> @rev/objects :object/text :schema)
;;        tx-obj (rev/object-initiate! schema connection)
;;        obj (rev/object-get schema connection (:db/id tx-obj))
;;        tx-rdata (rev/page-new-object! (assoc rdata :object-id (:db/id tx-obj)))
;;        page1 (rev/page-get tx-rdata)]
;;    (rev/object-set! schema connection (:db/id obj) {:reverie/area :a})
;;    (rev/object-render schema connection (:db/id tx-obj) tx-rdata))
;;  => [:text "my initial text" :image "my initial image"])


;; (fact
;;  "object-copy!"
;;  (let [schema (-> @rev/objects :object/text :schema)
;;        obj1 (-> rdata rev/page-objects first)
;;        obj2 (rev/object-get schema connection (:db/id (rev/object-copy! schema connection (:db/id obj1))))]
;;    (let [obj1 (select-keys obj1 (conj (keys obj1) :db/id))
;;          obj2 (select-keys obj2 (conj (keys obj2) :db/id))]
;;      (and
;;       (number? (:db/id obj1))
;;       (number? (:db/id obj2))
;;       (not= (:db/id obj1) (:db/id obj2))
;;       (= (dissoc obj1 :db/id :reverie/order) (dissoc obj2 :db/id :reverie/order)))))
;;  => true)

;; (fact
;;  "object-move!"
;;  (let [schema (-> @rev/objects :object/text :schema)
;;        obj1 (-> rdata rev/page-objects first)
;;        tx-obj2 (rev/object-move! schema connection (:db/id obj1) {:page-id (:page-id page2)
;;                                                                   :area :b})
;;        obj2 (-> (assoc rdata :page-id (:page-id page2)) rev/page-objects first)]
;;    (and
;;     (= (:db/id page2) (-> obj2 :reverie.page/_objects first :page-id))
;;     (= (:db/id page1) (-> obj1 :reverie.page/_objects first :page-id))))
;;  => true)
