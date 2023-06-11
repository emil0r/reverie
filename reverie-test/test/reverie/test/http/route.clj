(ns reverie.test.http.route
  (:require [reverie.http.route :as route]
            [midje.sweet :refer :all]
            [ring.mock.request :refer :all]))

(fact
 "route matches"
 (let [r (route/route ["/foo/:bar" {:get {:match {:bar #"\d+"}
                                          :params {:body [:map [:x :int] [:y :int]]
                                                   :path {:bar :int}
                                                   :query {:x :int
                                                           :y :int}}
                                          :responses {200 {:body [:map [:enum :total] :int]}}
                                          :handler str}}])
       match1 (route/match?
               r (assoc (request :get "/foo/1234" {:foo "bar"})
                        :params {:foo "bar"}))
       match2 (route/match?
               r (request :get "/"))
       match3 (route/match?
               (route/route ["/foo/:bar" {:get {:handler str}}])
               (request :get "/foo/1234"))
       match4 (route/match?
               (route/route "/foo" ["/:bar" {:get {:handler str}}])
               (assoc (request :get "/foo/1234")
                      :shortened-uri "/1234"))]
   (fact "match1: hit"
         (nil? match1) => false)
   (fact "match2: no hit"
         (nil? match2) => true)
   (fact "match3: bar has been caught"
         (some? match3) => true)
   (fact "match4: bar has been caught"
         (some? match4) => true)))
