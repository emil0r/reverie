(ns reverie.test.core
  (:require [reverie.core :as rev]
            [hiccup.core :as hiccup]
            [fs.core :as fs])
  (:use midje.sweet
        [datomic.api :only [q db] :as d]
        [ring.mock.request]))


(def db-uri-mem "datomic:mem://reverie")

(defn setup []
  (d/delete-database db-uri-mem)
  (let [database (d/create-database db-uri-mem)
        connection (d/connect db-uri-mem)
        schema (read-string (slurp "schema/datomic.schema"))]
    @(d/transact connection schema)
    {:database database
     :connection connection}))

(defn pre-test [request]
  (keys request))

(defn post-test [request]
  (keys request))

(defn reset-routes! []
  (reset! rev/routes {}))

(defn reset-templates! []
  (reset! rev/templates {}))

(defn reset-objects! []
  (reset! rev/objects {}))


(fact
 "deftemplate"
 (do
   (reset-templates!)
   (rev/deftemplate main {:areas [:a :b :c] :pre [pre-test] :post [post-test]} "body")
   (let [m @rev/templates
         k (first (keys m))
         options (:options (m k))
         func (:fn (m k))]
     [k options (:body (func {:request (request :get "/")
                              :connection nil
                              :page-id nil
                              :mode :edit}))])) => [:main
                                                    {:areas [:a :b :c]
                                                     :pre [pre-test]
                                                     :post [post-test]}
                                                    "body"])


(fact
 "objectfuncs simple"
 (let [obj (rev/object-funcs [] [:get :post] (clojure.string/join " " ["this" "is" "my" "function!"]))]
   [((:get obj) :rdata-placeholder {}) ((:post obj) :rdata-placeholder {})]) => ["this is my function!" "this is my function!"])

(fact
 "objectfuncs multiple method/fn"
 (let [obj (rev/object-funcs [] [:get fn-get :post fn-post]
                             [fn-get "my get"]
                             [fn-post "my post"])]
   [((:get obj) :rdata-placeholder {}) ((:post obj) :rdata-placeholder {})]) => ["my get" "my post"])

(fact
 "objectfuncs attributes"
 (let [obj (rev/object-funcs [text] [:get] (hiccup/html [:div "this is my " text]))]
   ((:get obj) :rdata-placeholder {:text "text"})) => "<div>this is my text</div>")


(fact
 "defobject"
 (do
   (reset-objects!)
   (rev/defobject object/text {:areas [:a :b] :attributes [{:text {:db/ident :object.text/text :db/type :db.type/string :db/cardinality :db.cardinality/one :db/doc "Text of the text object"} :initial "" :input :text :name "Text"}]} [:get] "")
   (-> @rev/objects :object/text nil?)) => false)



(fact
 "defobject and run-schemas!"
 (let [{:keys [database connection]} (setup)]
   (reset-objects!)
   (rev/defobject object/text {:areas [:a :b] :attributes [{:text {:db/ident :object.text/text :db/valueType :db.type/string :db/cardinality :db.cardinality/one :db/doc "Text of the text object"} :initial "" :input :text :name "Text" :description ""}]} [:get] "")
   (rev/run-schemas! connection)
   (number? (ffirst (q '[:find ?c :where [?c :db/ident :object.text/text]] (db connection))))) => true)


(fact
 "defobject and atttributes"
 (let [{:keys [database connection]} (setup)]
   (reset-objects!)
   (rev/defobject object/text {:areas [:a :b] :attributes [{:text {:db/ident :object.text/text :db/valueType :db.type/string :db/cardinality :db.cardinality/one :db/doc "Text of the text object"} :initial "" :input :text :name "Text" :description ""}]} [:get] text)
   (rev/run-schemas! connection)
   (let [f (-> @rev/objects :object/text :get)]
     (f {:uri "/"} {:text "my text"}))) => "my text")

(fact
 "routes"
 (reset-routes!)
 (rev/add-route! "/" {:page-id 1 :type :normal})
 (rev/add-route! "/info" {:page-id 2 :type :normal})
 (rev/add-route! "/contact" {:page-id 3 :type :normal})
 (rev/add-route! "/apps/app1" {:page-id 4 :type :app})
 (rev/add-route! "/apps/app2" {:page-id 5 :type :app})
 (map #(-> % second :page-id) [(rev/get-route "/")
                               (rev/get-route "/contact")
                               (rev/get-route "/info")
                               (rev/get-route "/apps/app1")
                               (rev/get-route "/apps/app2/asdf")])
 => [1 3 2 4 5])

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
  [:post ["*" {:keys [testus] :as data}] (= testus true)]
  [:post ["*" data] (rev/raise-response {:status 404 :body "404, page not found"})]
  )

(fact
 "defapp"
 (let [app (:gallery @rev/apps)
       [[_ _ _ g1] [_ _ _ g2] [_ _ _ g3]
        [_ _ _ p1] [_ _ _ p2] [_ _ _ p3] [_ _ _ p4] [_ _ _ p5] [_ _ _ p6]] (:fns app)]
   [(:body (g1 {} {:gallery "gallery" :image "image"}))
    (:body (g2 {} {:gallery "gallery"}))
    (:body (g3 {} {}))
    (:body (p1 {} {:gallery "gallery"} "my data"))
    (:body (p2 {} {:gallery "gallery2"} "my data"))
    (:body (p3 {} {:gallery "gallery3"} "my data"))
    (:body (p4 {} {} "my data here"))
    (:body (p5 {} {} {:testus true}))
    (p6 {} {} {:not :this})
    ])
 => ["gallery/image"
     "this is my gallery"
     "base"
     "gallery, my post -> my data"
     "gallery2, my post -> my data"
     "gallery3, my post -> my data"
     "my post -> my data here"
     true
     {:status 404 :body "404, page not found"}
     ])
