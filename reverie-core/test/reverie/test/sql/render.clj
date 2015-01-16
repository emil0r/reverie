(ns reverie.test.sql.render
  (:require [com.stuartsierra.component :as component]
            [hiccup.core :as hiccup]
            [reverie.render :as render]
            [reverie.core :refer [deftemplate area defapp]]
            reverie.sql.objects.text
            reverie.sql.objects.image
            [reverie.page :as page]
            [reverie.site :as site]
            [reverie.test.database.sql :refer [seed! get-db]]
            [ring.mock.request :refer :all]
            [midje.sweet :refer :all]))



(defn- template-render [request page properties params]
  [:html
   [:head
    [:meta {:charset "UTF-8"}]
    [:title (page/name page)]]
   [:body
    [:div (area a)]
    [:div (area b)]]])

(defn- expected [title a b]
  (hiccup/html
   [:html
    [:head
     [:meta {:charset "UTF-8"}]
     [:title title]]
    [:body
     [:div a]
     [:div b]]]))

(defn- baz-get [request page properties {:keys [caught]}]
  {:a (str "baz get " caught)
   :b (str "baz get " caught)})
(defn- baz-post [request page properties {:keys [caught]}]
  {:a (str "baz post " caught)
   :b (str "baz post " caught)})
(defn- baz-any [request page properties {:keys [caught]}]
  {:a (str "baz any " caught)
   :b (str "baz any " caught)})

(deftemplate foobar template-render)
(deftemplate foobaz template-render)
(defapp baz {} [["/" {:get baz-get :post baz-post :any baz-any}]
                ["/:caught" {:get baz-get :post baz-post :any baz-any}]])


(fact "render object"
      (seed!)
      (let [db (get-db)]
        (fact "object text"
              (render/render
               (first (page/objects (page/get-page db 1)))
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
                      (list "baz get " [:p "Text2"])
                      (list [:img {:src "/path/to/img.jpg"
                                   :title "TitleImage1"
                                   :alt "AltImage1"}]
                            "baz get "
                            [:p "Text3"])))
   (fact "rendered page, uri /baz/caught-this"
         (:body
          (render/render site {:uri "/baz/caught-this"
                               :server-name "example.com"
                               :request-method :post}))
         => (expected "Baz"
                      (list "baz post caught-this" [:p "Text2"])
                      (list [:img {:src "/path/to/img.jpg"
                                   :title "TitleImage1"
                                   :alt "AltImage1"}]
                            "baz post caught-this"
                            [:p "Text3"])))))
