(ns reverie.test.scenarios.scenario1
  (:require [reverie.core :as rev]
            [reverie.schema.datomic :as _] ;; for reloads in midje
            )
  (:use midje.sweet
        ;;[reverie.test.core :only [setup]]
        ))


(fact
 "defining template"
 (let [;;{:keys [connection]} (setup)
       rdata (rev/reverie-data {})
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
   (:body ((-> template :main :fn) rdata)))
 => ["<!DOCTYPE html>"
     [:html
      [:head
       [:meta {:charset "utf-8"}]
       [:title "Scenario 1"]]
      [:body
       [:div.area-a []]
       [:div.area-b []]
       [:div.area-c []]]]])
