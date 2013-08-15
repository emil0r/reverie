(ns reverie.test.middleware
  (:require [reverie.middleware :as middleware]
            [reverie.auth.user :as user]
            [noir.session :as session]
            [noir.cookies :as cookies])
  (:use midje.sweet
        ring.mock.request
        reverie.test.helpers))


(defn test-handler [request]
  {:status 200
   :headers {"Location" "http://localhost"}
   :body "Hello World!"})

(fact
 "access admin"
 (with-noir
   (user/login "admin" "admin0r")
   (:status ((-> test-handler
                 middleware/wrap-admin) (request :get "/admin/test")))) => 200)

(fact
 "access admin, failure"
 (with-noir
  (:status ((-> test-handler
                middleware/wrap-admin) {}))) => 302)
