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
