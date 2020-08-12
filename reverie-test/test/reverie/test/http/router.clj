(ns reverie.test.http.router
  (:require [com.stuartsierra.component :as component]
            [reverie.core :refer [defpage]]
            [reverie.page :as page]
            [reverie.http.router :as router]
            [reverie.test.database.sql-helpers :refer [start-db stop-db seed!]]
            [reverie.test.helpers.render-functions :refer [get-fn
                                                           get-fn-exception
                                                           get-fn-simple]]
            [midje.sweet :refer :all]
            [ring.mock.request :refer :all]))


(fact
 "router"
 (let [db (start-db)
       router (component/start (router/router {:database db}))]
   (try
     (seed! db)
     (do
       (defpage "/test/http/router"
         {}
         [["/" {:get get-fn}]]))
     (fact "get-page /test/http/router"
           (page/type (router/get-page router (request :get "/test/http/router")))
           => :raw)
     (fact "get-page /"
           (page/type (router/get-page router (request :get "/")))
           => :page)
     (finally
       (stop-db db)))))
