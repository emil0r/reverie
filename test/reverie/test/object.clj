(ns reverie.test.object
  (:require [korma.core :as korma]
            [reverie.entity :as entity]
            [reverie.object :as object]
            [reverie.page :as page])
  (:use midje.sweet
        reverie.test.init
        ring.mock.request))

(fact
 "object/add!"
 (let [p (page/get {:serial 1 :version 0})]
   (:name (object/add! {:page-id (:id p)} {:name "text" :area :a} {:text "testus"})))
 => "text")

(fact
 "object/get"
 (:text (object/get {:serial 1 :version 0})) => "testus")

(fact
 "object/render"
 (object/render (merge (request :get "/") {:serial 1 :version 0})) => "testus")

