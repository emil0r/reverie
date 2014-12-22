(ns reverie.test.template
  (:require [reverie.template :as template]
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
   (render t (request :get "/") nil nil))
 => {:status 200
     :body "OK!"})
