(ns reverie.test.response
  (:require [reverie.response :as response]
            [midje.sweet :refer :all]))



(fact
 "200"
 (response/get
  {:status 200 :headers {} :body "OK!"})
 => {:status 200 :headers {} :body "OK!"})

(fact
 "from long"
 (fact
  "404"
  (response/get 404)
  => {:status 404 :headers {} :body "404, Page Not Found"})
 (fact
  "301"
  (response/get 301 "http://www.example.com/")
  => {:status 301 :headers {"Location" "http://www.example.com/"} :body ""}))
