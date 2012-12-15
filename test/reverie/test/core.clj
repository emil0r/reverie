(ns reverie.test.core
  (:require [reverie.core :as rev])
  (:use midje.sweet))


(defn pre-test [request]
  (keys request))

(defn post-test [request]
  (keys request))


(defn reset-routes! []
  (reset! rev/routes {}))

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
   [((:get obj) {}) ((:post obj) {})]) => ["this is my function!" "this is my function!"])

(fact
 "objectfuncs multiple method/fn"
 (let [obj (rev/object-funcs [] [:get fn-get :post fn-post]
                             [fn-get "my get"]
                             [fn-post "my post"])]
   [((:get obj) {}) ((:post obj) {})]) => ["my get" "my post"])

(fact
 "objectfuncs attributes"
 (let [obj (rev/object-funcs [text] [:get] (str "this is my " text))]
   ((:get obj) {} "text")) => "this is my text")
