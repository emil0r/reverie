(ns reverie.test.middleware
  (:require [reverie.middleware :as middleware]
            [reverie.auth.user :as user]
            [noir.session :as session]
            [noir.cookies :as cookies]
            [clojure.edn])
  (:use midje.sweet
        [reverie.util :only [generate-handler]]
        [ring.middleware.edn :only [wrap-edn-params]]
        [ring.middleware.keyword-params :only [wrap-keyword-params]]
        [ring.middleware.params :only [wrap-params]]
        ring.mock.request
        reverie.test.helpers))


(defn test-handler [request]
  {:status 200
   :headers {"Location" "http://localhost"}
   :body "Hello World!"})

(defn ping-handler [request]
  {:status 200
   :headers {"Location" "http://localhost"}
   :body request})

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
                middleware/wrap-admin) (request :get "/admin")))) => 302)

(fact
 "wrap edn params"
 (with-noir
   (let [new-handler (generate-handler [[wrap-params]
                                        [wrap-edn-params]] ping-handler)
         req (content-type (request :post "/edn-params" "{:test [1 2 3]}") "application/edn")]
     (-> (new-handler req) :body :params)))
  => {:test [1 2 3]})
