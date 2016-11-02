(ns reverie.test.page
  (:require
   [clj-time.core :as t]
   [hiccup.page :refer [html5]]
   [reverie.core :refer [area]]
   [reverie.page :as page]
   [reverie.route :as route]
   [reverie.render :as render]
   [reverie.system :as sys]
   [reverie.template :as template]
   [midje.sweet :refer :all]))


(defn get-renderer
  ([]
   (get-renderer nil))
  ([routes]
   (render/map->Renderer {:name ::renderer :options {:render-fn :hiccup} :methods-or-routes routes})))


(defn template-fn [request page properties params]
  (html5
   [:head [:title "Hi!"]]
   [:body (area a)]))


(defn get-template []
  (template/template template-fn))

(defn get-app-page [template renderer routes]
  (page/map->AppPage {:route (route/route ["/foo"])
                      :app "app"
                      :app-routes routes
                      :app-area-mappings nil
                      :slug "/foo"
                      :id 1
                      :serial 1
                      :name "Test"
                      :title ""
                      :properties {}
                      :options {:renderer renderer}
                      :template template
                      :created (t/now)
                      :updated (t/now)
                      :parent nil
                      :data nil
                      :version 0
                      :published-data (t/now)
                      :published? true
                      :objects []
                      :raw-data nil}))

(defn get-raw-page [template renderer routes]
  (page/map->RawPage {:route (route/route ["/foo"])
                      :routes routes
                      :database nil
                      :options {:template template
                                :renderer renderer}}))


(defn http-any [request page properties & [params]]
  {:a [:div "Hi there!"]})


(fact
 "Simple renderer with just a :render-fn defined"
 (let [template (get-template)
       routes [(route/route ["/" ^:meta {:name ::index} {:any http-any}])]
       renderer (get-renderer)]
   (swap! sys/storage assoc-in [:renderers :reverie.system/override] {})
   (swap! sys/storage assoc-in [:renderers ::renderer] renderer)
   (render/render (get-app-page template ::renderer routes) {:uri "/foo"}))
 => {:body "<!DOCTYPE html>\n<html><head><title>Hi!</title></head><body><div>Hi there!</div></body></html>"
     :headers {"Content-Type" "text/html; charset=utf-8;"}
     :status 200})


(defn advanced-any [request page properties params]
  {:what "Advanced"})

(defn present-advanced-any [data]
  {:a [:div "What: " (:what data)]})

(fact
 "Advanced renderer with routes"
 (let [template (get-template)
       routes [(route/route ["/" ^:meta {:name ::index} {:any advanced-any}])]
       renderer-routes {::index {:any present-advanced-any}}
       renderer (get-renderer renderer-routes)]
   (swap! sys/storage assoc-in [:renderers :reverie.system/override] {})
   (swap! sys/storage assoc-in [:renderers ::renderer] renderer)
   (render/render (get-app-page template ::renderer routes) {:uri "/foo"}))
 => {:body "<!DOCTYPE html>\n<html><head><title>Hi!</title></head><body><div>What: Advanced</div></body></html>"
     :headers {"Content-Type" "text/html; charset=utf-8;"}
     :status 200})

(defn override-present-advanced-any [data]
  {:a [:div "[Override] What: " (:what data)]})

(fact
 "Advanced renderer with routes and an override"
 (let [template (get-template)
       routes [(route/route ["/" ^:meta {:name ::index} {:any advanced-any}])]
       renderer-routes {::index {:any present-advanced-any}}
       renderer (get-renderer renderer-routes)
       override-renderer-routes {::index {:any override-present-advanced-any}}
       override-renderer (get-renderer override-renderer-routes)]
   (swap! sys/storage assoc-in [:renderers :reverie.system/override] {})
   (swap! sys/storage assoc-in [:renderers ::renderer] renderer)
   (swap! sys/storage assoc-in [:renderers ::override-renderer] override-renderer)
   (swap! sys/storage assoc-in [:renderers :reverie.system/override ::renderer] ::override-renderer)
   (render/render (get-app-page template ::renderer routes) {:uri "/foo"}))
 => {:body "<!DOCTYPE html>\n<html><head><title>Hi!</title></head><body><div>[Override] What: Advanced</div></body></html>"
     :headers {"Content-Type" "text/html; charset=utf-8;"}
     :status 200})


(fact
 "Simple renderer with just a :render-fn defined"
 (let [template (get-template)
       routes [(route/route ["/" ^:meta {:name ::index} {:any http-any}])]
       renderer (get-renderer)]
   (swap! sys/storage assoc-in [:renderers :reverie.system/override] {})
   (swap! sys/storage assoc-in [:renderers ::renderer] renderer)
   (swap! sys/storage assoc-in [:templates ::template] template)
   (render/render (get-raw-page ::template ::renderer routes) {:uri "/foo"}))
 => {:body "<!DOCTYPE html>\n<html><head><title>Hi!</title></head><body><div>Hi there!</div></body></html>"
     :headers {"Content-Type" "text/html; charset=utf-8;"}
     :status 200})


(defn raw-http-any [request properties params]
  [:div "Hi there!"])

(fact
 "Simple renderer with just a :render-fn defined and no template"
 (let [routes [(route/route ["/" ^:meta {:name ::index} {:any raw-http-any}])]
       renderer (get-renderer)]
   (swap! sys/storage assoc-in [:renderers :reverie.system/override] {})
   (swap! sys/storage assoc-in [:renderers ::renderer] renderer)
   (render/render (get-raw-page nil ::renderer routes) {:uri "/foo"}))
 => {:body "<div>Hi there!</div>"
     :headers {"Content-Type" "text/html; charset=utf-8;"}
     :status 200})


(defn raw-http-any-exact [request properties params]
  {:body "raw text"
   :headers {"Content-Type" "plain/text"}
   :status 200})

(fact
 "Simple renderer with just a :render-fn defined and a method that returns exactly what it wants"
 (let [routes [(route/route ["/" ^:meta {:name ::index} {:any raw-http-any-exact}])]
       renderer (get-renderer)]
   (swap! sys/storage assoc-in [:renderers :reverie.system/override] {})
   (swap! sys/storage assoc-in [:renderers ::renderer] renderer)
   (render/render (get-raw-page nil ::renderer routes) {:uri "/foo"}))
 => {:body "raw text"
     :headers {"Content-Type" "plain/text"}
     :status 200})
