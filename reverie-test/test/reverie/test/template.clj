(ns reverie.test.template
  (:require [reverie.template :as template]
            [reverie.page :as page]
            [reverie.render :refer [render]]
            [ring.mock.request :refer :all]
            [midje.sweet :refer :all])
  (:import [reverie.template Template]))


(defn- testus [request page propeties params]
  {:status 200
   :body "OK!"})


(fact
 "render template works"
 (let [t (Template. testus)]
   (render t (request :get "/") (page/map->Page {})))
 => {:status 200
     :body "OK!"})
