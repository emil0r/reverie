(ns reverie.routes
  (:require [reitit.frontend :as r.frontend]
            [reitit.coercion.spec :as coercion.spec]
            [reverie.views.root]))


(def routes
  [["/"
    {:name :root/index
     :view reverie.views.root/index}]
   
   ])

(def router
  (r.frontend/router routes {:data {:coercion coercion.spec/coercion}}))

