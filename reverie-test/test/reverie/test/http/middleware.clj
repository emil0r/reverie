(ns reverie.test.http.middleware
  (:require [reverie.core :refer [defpage]]
            [reverie.helpers.middleware :refer [add-page-middleware
                                                create-handler
                                                merge-handlers]]
            [reverie.http.route :as route]
            [reverie.page :as page]
            [reverie.system :as sys]
            [midje.sweet :refer :all]
            [ring.mock.request :refer :all]))


(defn wrap-x [handler x]
  (fn [request]
    (let [response (handler request)]
      (update-in response [:wrap-x] conj x))))

(defn get-fn [request page params]
  :get-fn)

(fact
 "add-page-middleware"
 (fact "merging middleware from options of page to options in routes"
       (let [page (->> {:route (route/route ["/"])
                        :options {:middleware [[wrap-x :wrapped-options]]}
                        :routes (map route/route [["/foo/bar" ^:meta {:middleware [[wrap-x "foo"]
                                                                                   [wrap-x "bar"]]} {:get get-fn}]
                                                  ["/" ;; ^:meta {:middleware [[wrap-x "/"]]}
                                                   {:get get-fn}]])}
                       (page/raw-page)
                       (add-page-middleware))
             handler (-> page :routes first :options :middleware)]
         (:wrap-x (handler (request :get "/foo/bar")))
         => ["bar" "foo" :wrapped-options])))

(fact
 "defpage with middleware"
 (do
   (defpage "/test/http/middleware"
     {:middleware [[wrap-x :test/http/middleware]]}
     [["/" {:get get-fn}]]))
 (let [req (request :get "/test/http/middleware")
       handler (-> (sys/raw-page "/test/http/middleware")
                   (page/handler req))]
   (-> (handler req)
       :wrap-x
       first) => :test/http/middleware))
