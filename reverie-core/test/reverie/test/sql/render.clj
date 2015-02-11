(ns reverie.test.sql.render
  (:require [com.stuartsierra.component :as component]
            [hiccup.core :as hiccup]
            [reverie.render :as render]
            [reverie.core :refer [deftemplate area defapp]]
            [reverie.database :as db]
            reverie.sql.objects.text
            reverie.sql.objects.image
            [reverie.page :as page]
            [reverie.site :as site]
            [reverie.test.database.sql-helpers :refer [seed! get-db]]
            [reverie.test.helpers :refer [expected]]
            [ring.mock.request :refer :all]
            [midje.sweet :refer :all]))


(fact "render object"
      (seed!)
      (let [db (get-db)]
        (fact "object text"
              (render/render
               (first (page/objects (db/get-page db 1)))
               {})
              => [:p "Text1"])))


(fact
 "rendering with site"
 (seed!)
 (let [db (get-db)
       site (component/start
             (site/site {:host-names ["example.com"]
                         :database db
                         :system (:system db)
                         :render-fn (fn [data] (hiccup.compiler/render-html data))}))]
   (fact "404 due to wrong host name"
         (:status (render/render site {:uri "/"})) => 404)
   (fact "rendered page, uri /"
         (:body
          (render/render site {:uri "/" :server-name "example.com"
                               :request-method :get}))
         => (expected "Main" [:p "Text1 (publ)"] nil))
   (fact "rendered page, uri /baz"
         (:body
          (render/render site {:uri "/baz" :server-name "example.com"
                               :request-method :get}))
         => (expected "Baz"
                      (list "baz get " [:p "Text2 (publ)"])
                      (list "baz get "
                            [:p "Text3 (publ)"])))
   (fact "rendered page, uri /baz/caught-this"
         (:body
          (render/render site {:uri "/baz/caught-this"
                               :server-name "example.com"
                               :request-method :post}))
         => (expected "Baz"
                      (list "baz post caught-this")
                      (list [:img {:src "/path/to/img.jpg"
                                   :title "TitleImage1 (publ)"
                                   :alt "AltImage1 (publ)"}]
                            "baz post caught-this")))))
