(ns reverie.test.schema.datomic.page
  (:require [reverie.core :as rev]
            [reverie.schema.datomic :as _] ;; for reloads in midje
            )
  (:use midje.sweet
        [datomic.api :only [q db] :as d]
        [reverie.test.core :only [setup]]
        [ring.mock.request])
  (:import reverie.core.ObjectSchemaDatomic reverie.core.ReverieDataDatomic))


(reset! rev/routes {})
(reset! rev/templates {})
(reset! rev/objects {})
(reset! rev/apps {})

(rev/defobject object/text {:areas [:a :b :c]
                            :attributes [{:text {:db/ident :object.text/text
                                                 :db/valueType :db.type/string
                                                 :db/cardinality :db.cardinality/one
                                                 :db/doc "Text of the text object"}
                                          :initial ""
                                          :input :text
                                          :name "Text"
                                          :description ""}]}
  [:get]
  text)
(rev/run-schemas! (:connection (setup)))

(rev/deftemplate :main [:areas [:a :b :c]]
  (list "<!DOCTYPE html>"
        [:html
         [:head
          [:meta {:charset "utf-8"}]
          [:title "page.clj"]]
         [:body
          [:div.area-a (rev/area :a)]
          [:div.area-b (rev/area :b)]
          [:div.area-c (rev/area :c)]]]))

(rev/defapp gallery {}
  ;; test :get
  [:get ["/:gallery/:image" {:gallery #"\w+" :image #"\d+"}] (clojure.string/join "/" [gallery image])]
  [:get ["/:gallery" {:gallery #"\w+"} {:wrap [nil]}] (str "this is my " gallery)]
  [:get ["*"] "base"]
  ;; test order in methods array
  [:post ["/:gallery" {:gallery #"\w+"} data] (str gallery ", my post -> " data)]
  [:post ["/:gallery" {:gallery #"\w+"} {:wrap [nil]} data] (str gallery ", my post -> " data)]
  [:post ["/:gallery" {:wrap [nil]} data] (str gallery ", my post -> " data)]
  [:post ["*" data] (str "my post -> " data)]
  ;; deconstructing works
  [:post ["*" {:keys [testus] :as data}] (= testus true)])

(defn- init-data [command data]
  (let [my-tx-data {:reverie.page/name "my test page"
                    :reverie.page/uri "/my-test-page"
                    :reverie.page/template :main}]
    (if-let [tx-data (:tx-data data)]
      (merge {:command command
              :parent nil
              :tx-data (merge my-tx-data tx-data)
              :rights :?} data)
      (merge {:command command
              :parent nil
              :tx-data my-tx-data
              :rights :?} data))))

(fact
 "add page"
 (let [{:keys [connection]} (setup)
       data (init-data :page-new {:connection connection
                                  :request (request :get "/")})
       rdata (rev/reverie-data data)]
   (-> rdata rev/page-new! :page-id pos?))
 => true)

(fact
 "get page"
 (let [{:keys [connection]} (setup)
       data (init-data :page-new {:connection connection
                                  :request (request :get "/")})
       rdata (rev/reverie-data data)
       new-page-id (-> rdata rev/page-new! :page-id)]
   (= new-page-id (:db/id (rev/page-get (assoc rdata :page-id new-page-id)))))
 => true)

(fact
 "update page, delete page & restore page"
 (let [{:keys [connection]} (setup)
       data (init-data :page-new {:connection connection
                                  :request (request :get "/")})
       rdata (rev/reverie-data data)
       tx-rdata (rev/page-new! rdata)
       page (rev/page-get tx-rdata)
       tx-update (rev/page-update! (assoc tx-rdata :tx-data {:reverie.page/name "my updated page"}))
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
       data (init-data :page-new {:connection connection
                                  :request (request :get "/")})
       rdata (rev/reverie-data data)
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
       tx-rdata2 (rev/page-new-object! (assoc tx-rdata :object-id obj-id))
       page (rev/page-get tx-rdata2)
       object (rev/object-get obj connection obj-id)]
   (= {:object-id obj-id
       :area :a}
      {:object-id (-> page :reverie.page/objects first :db/id)
       :area (-> object :reverie/area)})) => true)


(fact
 "delete object from page"
 (let [{:keys [connection]} (setup)
       data (init-data :page-new {:connection connection
                                  :request (request :get "/")})
       rdata (rev/reverie-data data)
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
       tx-rdata2 (-> (assoc tx-rdata :object-id obj-id)
                     rev/page-new-object!
                     rev/page-delete-object!)
       object (rev/object-get obj connection obj-id)]
   (:reverie/active? object)) => false)

(fact
 "list objects of page"
 (let [{:keys [connection]} (setup)
       data (init-data :page-new {:connection connection
                                  :request (request :get "/")})
       rdata (rev/reverie-data data)
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
       tx-rdata2 (rev/page-new-object! (assoc tx-rdata :object-id obj-id1))
       tx-rdata3 (rev/page-new-object! (assoc tx-rdata :object-id obj-id2))
       page (rev/page-get tx-rdata2)
       objects (rev/page-objects (assoc tx-rdata :area :a))]
   (vec (map :object.text/text objects))) => ["obj-1", "obj-2"])


(fact
 "page render"
 (let [{:keys [connection]} (setup)
       rdata (rev/reverie-data (init-data :page-new {:connection connection
                                                     :request (request :get "/my-test-page")}))
       tx-rdata (rev/page-new! rdata)
       obj (:schema (:object/text @rev/objects))
       tx-obj (rev/object-upgrade! obj connection)
       obj-id1 (:db/id (rev/object-initiate! obj connection))
       obj-id2 (:db/id (rev/object-initiate! obj connection))
       obj-id3 (:db/id (rev/object-initiate! obj connection))
       tmp (rev/object-set! obj connection {:reverie/area :a :reverie/order 1 :text "obj-1"} obj-id1)
       tmp (rev/object-set! obj connection {:reverie/area :a :reverie/order 2 :text "obj-2"} obj-id2)
       tmp (rev/object-set! obj connection {:reverie/area :a :reverie/order 3 :text "obj-3"} obj-id3)
       tx-rdata2 (rev/page-new-object! (assoc tx-rdata :object-id obj-id1))
       tx-rdata3 (rev/page-new-object! (assoc tx-rdata :object-id obj-id2))
       tx-rdata4 (rev/page-new-object! (assoc tx-rdata :object-id obj-id3))
       page (rev/page-get tx-rdata2)
       rendered (rev/page-render rdata)]
   rendered) =>
   ["<!DOCTYPE html>"
                 [:html
                  [:head
                   [:meta {:charset "utf-8"}]
                   [:title "page.clj"]]
                  [:body
                   [:div.area-a ["obj-1" "obj-2" "obj-3"]]
                   [:div.area-b []]
                   [:div.area-c []]]]])

(fact
 "page-render with app"
 (let [{:keys [connection]} (setup)
       rdata (rev/reverie-data (init-data :page-new {:connection connection
                                                     :request (request :get "/gallery/garden/image1")
                                                     :tx-data {:reverie.page/uri "/gallery"
                                                               :reverie.page/app :gallery}
                                                     :page-type :app}))
       tx-rdata (rev/page-new! rdata)]
   (rev/page-render rdata)) => "/garden/image1")
