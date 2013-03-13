(ns reverie.test.scenarios.scenario1
  (:require [reverie.core :as rev]
            [reverie.schema.datomic :as _] ;; for reloads in midje
            )
  (:use midje.sweet
        [datomic.api :only [q db] :as d]
        [reverie.test.core :only [setup]])
  (:import reverie.core.ObjectSchemaDatomic))


(fact
 "defining template"
 (let [{:keys [connection]} (setup)
       rdata (rev/reverie-data {:connection connection})
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
   ((-> template :main :fn) rdata)
   ) => nil)
