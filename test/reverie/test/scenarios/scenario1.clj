(ns reverie.test.scenarios.scenario1
  (:require [reverie.core :as rev]
            [reverie.page :as page]
            [reverie.server :as server])
  (:use midje.sweet
        ring.mock.request
        ))


(fact
 "defining template"
 (let [p (page/get {:serial 1 :version 0})
       req (assoc (request :get "/test") :page-id (:id p))
       template (rev/deftemplate :main [:areas [:a :b :c]]
                  (list "<!DOCTYPE html>"
                        [:html
                         [:head
                          [:meta {:charset "utf-8"}]
                          [:title "Scenario 1"]]
                         [:body
                          [:div.area-a (rev/area :a)]
                          [:div.area-b (rev/area :b)]
                          [:div.area-c (rev/area :c)]]]))]
   (:body ((-> template :main :fn) req)))
 => ["<!DOCTYPE html>"
     [:html
      [:head
       [:meta {:charset "utf-8"}]
       [:title "Scenario 1"]]
      [:body
       [:div.area-a []]
       [:div.area-b []]
       [:div.area-c []]]]])
