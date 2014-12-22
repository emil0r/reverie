(ns reverie.test.render
  (:require [hiccup.core :as hiccup]
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

(fact
 "test rendering with :page, manually setup"
 (let [req (request :get "/")
       t (template/template template-render)
       p (page/page {:template t :id 1 :name "Test page"
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
   (hiccup/html (render/render p req)))
 =>
 (hiccup/html
  [:html
   [:head
    [:meta {:charset "UTF-8"}]
    [:title "My test page"]]
   [:body
    [:div
     [:p "My text"]
     [:img {:alt "Alt" :src "/images/test.png" :title "Title"}]]]]))


(fact
 "test rendering with :app, manually setup"
 (let [req (request :get "/")
       t (template/template template-render)
       p (page/app-page {:template t :id 1 :name "Test page"
                         :title "My test page" :parent nil :children nil
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
   (hiccup/html (render/render p req)))
 =>
 (hiccup/html
  [:html
   [:head
    [:meta {:charset "UTF-8"}]
    [:title "My test page"]]
   [:body
    [:div
     [:p "My text"]
     [:p "app route base"]
     [:img {:alt "Alt" :src "/images/test.png" :title "Title"}]]]]))
