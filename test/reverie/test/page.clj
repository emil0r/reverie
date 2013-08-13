(ns reverie.test.page
  (:require [korma.core :as korma]
            [reverie.entity :as entity]
            [reverie.page :as page])
  (:use midje.sweet
        ring.mock.request))


(fact
 "page/new!"
 (let [p (page/new! {:tx-data {:updated (korma/sqlfn now) :serial 1
                               :name "Test" :title "Test"
                               :template :main :uri "/test"
                               :parent 0 :order 0 :version 0}})]
   (> (:page-id p) 0)) => true)

(fact
 "page/update!"
 (page/update! {:page-id 1 :tx-data {:name "Updated!"}})
 (:name (page/get {:page-id 1})) => "Updated!")
