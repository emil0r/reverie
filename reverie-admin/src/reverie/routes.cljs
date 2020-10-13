(ns reverie.routes
  (:require [reitit.frontend :as r.frontend]
            [reitit.coercion.spec :as coercion.spec]
            [reverie.views.profile]
            [reverie.views.root]))


(def routes
  [["/"
    {:name :root/index
     :view reverie.views.root/index}]
   ["/profile"
    {:name :profile/index
     :view reverie.views.profile/index}]
   
   ])

(def router
  (r.frontend/router routes {:data {:coercion coercion.spec/coercion}}))

