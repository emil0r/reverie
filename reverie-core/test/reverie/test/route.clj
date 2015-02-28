(ns reverie.test.route
  (:require [reverie.route :as route]
            [midje.sweet :refer :all]
            [ring.mock.request :refer :all]))


(fact
 "route matches"
 (let [r (route/route ["/foo/:bar" {:bar #"\d+"} {:bar Integer} {:get str}])
       match1 (route/match?
               r (assoc (request :get "/foo/1234" {:foo "bar"})
                   :params {:foo "bar"}))
       match2 (route/match?
               r (request :get "/"))
       match3 (route/match?
               (route/route ["/foo/:bar"]) (request :get "/foo/1234"))
       match4 (route/match?
               (route/route ["/:bar"]) (assoc (request :get "/foo/1234")
                                         :shortened-uri "/1234"))]
   (fact "match1: hit"
         (nil? match1) => false)
   (fact "match1: bar has been casted to an integer"
         (get-in match1 [:request :params :bar])
         => 1234)
   (fact "match2: no hit"
         (nil? match2) => true)
   (fact "match3: bar has been caught"
         (get-in match3 [:request :params :bar])
         => "1234")
   (fact "match4: bar has been caught"
         (get-in match4 [:request :params :bar])
         => "1234")))
