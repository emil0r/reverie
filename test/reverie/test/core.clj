(ns reverie.test.core
  (:require [reverie.core :as rev]
            [hiccup.core :as hiccup]
            [fs.core :as fs])
  (:use midje.sweet
        [datomic.api :only [q db] :as d]))


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

(defn reset-objects! []
  (reset! rev/objects {}))

(defn request []
  {:uri "/"})


(fact
 "deftemplate"
 (do
   (reset-routes!)
   (rev/deftemplate :main [:areas [:a :b :c] :pre [pre-test] :post [post-test]] "body")
   (let [m @rev/routes
         k (first (keys m))
         options (:options (m k))
         func (:fn (m k))]
     [k options (apply func (request))])) => [:main
                                              {:areas [:a :b :c]
                                               :pre [pre-test]
                                               :post [post-test]}
                                              "body"])


(fact
 "objectfuncs simple"
 (let [obj (rev/object-funcs [] [:get :post] (clojure.string/join " " ["this" "is" "my" "function!"]))]
   [((:get obj) request) ((:post obj) request)]) => ["this is my function!" "this is my function!"])

(fact
 "objectfuncs multiple method/fn"
 (let [obj (rev/object-funcs [] [:get fn-get :post fn-post]
                             [fn-get "my get"]
                             [fn-post "my post"])]
   [((:get obj) request) ((:post obj) request)]) => ["my get" "my post"])

(fact
 "objectfuncs attributes"
 (let [obj (rev/object-funcs [text] [:get] (hiccup/html [:div "this is my " text]))]
   ((:get obj) request "text")) => "<div>this is my text</div>")


(fact
 "defobject"
 (do
   (reset-objects!)
   (rev/defobject object/text [:areas [:a :b] :attributes [{:text {:db/ident :object.text/text :db/type :db.type/string :db/cardinality :db.cardinality/one :db/doc "Text of the text object"} :initial "" :input :text :name "Text"}]] [:get] "")
   (-> @rev/objects :object/text nil?)) => false)



(fact
 "defobject and run-schemas!"
 (let [{:keys [database connection]} (setup)]
   (reset-objects!)
   (rev/defobject object/text [:areas [:a :b] :attributes [{:text {:db/ident :object.text/text :db/valueType :db.type/string :db/cardinality :db.cardinality/one :db/doc "Text of the text object"} :initial "" :input :text :name "Text" :description ""}]] [:get] "")
   (rev/run-schemas! connection)
   (number? (ffirst (q '[:find ?c :where [?c :db/ident :object.text/text]] (db connection))))) => true)


(fact
 "defobject and atttributes"
 (let [{:keys [database connection]} (setup)]
   (reset-objects!)
   (rev/defobject object/text [:areas [:a :b] :attributes [{:text {:db/ident :object.text/text :db/valueType :db.type/string :db/cardinality :db.cardinality/one :db/doc "Text of the text object"} :initial "" :input :text :name "Text" :description ""}]] [:get] text)
   (rev/run-schemas! connection)
   (let [f (-> @rev/objects :object/text :get)]
     (f {:uri "/"} "my text"))) => "my text")
