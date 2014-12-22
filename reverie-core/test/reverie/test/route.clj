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
               r (request :get "/"))]
   (fact "match1 has been a hit"
         (nil? match1) => false)
   (fact "bar has been casted to an integer"
         (get-in match1 [:request :params :bar])
         => 1234)
   (fact "match2 has not been hit"
         (nil? match2) => true)))
