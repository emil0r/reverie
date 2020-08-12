(ns reverie.test.site
  (:require [com.stuartsierra.component :as component]
            [reverie.core :refer [defpage]]
            [reverie.cache :as cache]
            [reverie.http.router :as router]
            [reverie.render :as render]
            [reverie.site :as site]
            [reverie.test.database.sql-helpers :refer [start-db stop-db seed!]]
            [reverie.test.helpers.render-functions :refer [get-fn
                                                           get-fn-exception
                                                           get-fn-simple]]
            [midje.sweet :refer :all]
            [ring.mock.request :refer :all]))

(comment
  (def db (start-db))
  (stop-db db)

  (def router (component/start (router/router {:database db})))
  (component/stop router)
  (router/get-page router (request :get "/test/site"))
  (get @(:routes router) "/test/site")
  (router/get-route+properties "/" [(:routes router) router/static-routes])

  (keys @(:routes router))
  )

(fact
 "site"
 (let [db (start-db)
       router (component/start (router/router {:database db}))
       cachemanager (component/start (cache/get-cachemanager {:database db}))
       site (component/start (site/get-site {:hosts {"*" router}
                                             :cachemanager cachemanager
                                             :render-fn hiccup.compiler/render-html}))]

   (try
     (seed! db)
     (defpage "/test/site"
       {}
       [["/" {:get get-fn}]])
     (fact "getting RawPage"
           (let [result (render/render site (request :get "/test/site"))
                 j (juxt :status :body :headers)]
             (j result)) => [200 "get-fn" {"Content-Type" "text/html; charset=utf-8;"}])
     (fact "getting Page"
           (let [result (render/render site (request :get "/"))
                 j (juxt :status :body :headers)]
             (j result)) => [200
                             "<html><head><meta charset=\"UTF-8\" /><title>Main</title></head><body><div>Text1 (publ)</div><div></div></body></html>"
                             {"Content-Type" "text/html; charset=utf-8;"}])
     (finally
       (component/stop site)
       (component/stop cachemanager)
       (component/stop router)
       (stop-db db)))))

(let [])


(comment
  )
