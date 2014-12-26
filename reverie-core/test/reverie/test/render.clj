(ns reverie.test.render
  (:require [com.stuartsierra.component :as component]
            [hiccup.core :as hiccup]
            [reverie.area :as area]
            [reverie.object :as object]
            [reverie.page :as page]
            [reverie.render :as render]
            [reverie.route :as route]
            [reverie.site :as site]
            [reverie.template :as template]
            [ring.mock.request :refer :all]
            [midje.sweet :refer :all]))


(defn- template-render [request page properties params]
  [:html
   [:head
    [:meta {:charset "UTF-8"}]
    [:title (page/title page)]]
   [:body
    [:div
     (render/render (area/area :a) request page nil)]]])

(defn- app-route-base [request page properties params]
  {:a [:p "app route base"]})
(defn- app-route-foobar [request page properties {:keys [bar]}]
  {:a (list [:p "app route foobar"]
            [:p "bar is " bar])})

(defn- object-text-render [request object properties params]
  [:p (:text properties)])

(defn object-image-render [request object properties params]
  [:img {:src (:src properties) :alt (:alt properties) :title (:title properties)}])

(defn first-page []
  (let [t (template/template template-render)
        p (page/page {:template t :id 1 :name "Test page"
                      :route (route/route ["/"])
                      :title "My test page" :parent nil :children nil
                      :properties {}})
        objects [(object/object {:id 1 :name "Text" :area :a :page p
                                 :order -1
                                 :properties {:text "My text"}
                                 :methods {:any object-text-render}})
                 (object/object {:id 2 :name "Image" :area :a :page p
                                 :order 1
                                 :properties {:src "/images/test.png"
                                              :alt "Alt"
                                              :title "Title"}
                                 :methods {:get object-image-render}})]
        p (assoc p :objects objects)]
    p))

(defn first-page-app []
  (let [t (template/template template-render)
        p (page/app-page {:template t :id 1 :name "Test page"
                          :title "My test page" :parent nil :children nil
                          :route (route/route ["/"])
                          :properties {}
                          :app :my-test-app
                          :app-routes [(route/route ["/" {:get app-route-base}])
                                       (route/route ["/foo/:bar" {:get app-route-foobar}])]})
        objects [(object/object {:id 1 :name "Text" :area :a :page p
                                 :order -1
                                 :properties {:text "My text"}
                                 :methods {:any object-text-render}})
                 (object/object {:id 2 :name "Image" :area :a :page p
                                 :order 1
                                 :properties {:src "/images/test.png"
                                              :alt "Alt"
                                              :title "Title"}
                                 :methods {:get object-image-render}})]
        p (assoc p :objects objects)]
    p))

(defn expected-result
  ([]
     (expected-result nil))
  ([middle]
     (hiccup/html
      [:html
       [:head
        [:meta {:charset "UTF-8"}]
        [:title "My test page"]]
       [:body
        [:div
         [:p "My text"]
         middle
         [:img {:alt "Alt" :src "/images/test.png" :title "Title"}]]]])))

(fact
 "rendering with :page, manually setup"
 (let [req (request :get "/")
       p (first-page)]
   (hiccup/html (:body (render/render p req))))
 => (expected-result))


(fact
 "rendering with :app, manually setup"
 (let [req (request :get "/")
       p (first-page-app)]
   (hiccup/html (:body (render/render p req))))
 => (expected-result [:p "app route base"]))


(fact
 "rendering with site/Site"
 (let [p (first-page-app)
       s (component/start (site/site {:host-names ["example.com"]
                                      :render-fn (fn [data] (hiccup.compiler/render-html data))}))]
   (site/add-page! s p)
   (fact "get page"
         (site/get-page s {:uri "/"}) => p)
   (fact "get page /foo/bar"
         (site/get-page s {:uri "/foo/bar"}) => p)
   (fact "404 due to wrong host name"
         (render/render s {:uri "/"}) => (get @(:system-pages s) 404))
   (fact "rendered page, uri /"
         (:body
          (render/render s {:uri "/" :server-name "example.com"
                            :request-method :get}))
         => (expected-result [:p "app route base"]))
   (fact "rendered page, uri /foo/bar"
         (:body
          (render/render s {:uri "/foo/bar" :server-name "example.com"
                            :request-method :get}))
         => (expected-result (list [:p "app route foobar"]
                                   [:p "bar is bar"])))
   (fact "404 due to wrong path"
         (render/render s {:uri "/foo/bar/baz" :server-name "example.com"
                           :request-method :get})
         => (get @(:system-pages s) 404))))
