(ns reverie.test.module
  (:require
   [hiccup.page :refer [html5]]
   [reverie.core :refer [area]]
   [reverie.module :as module]
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


(defn get-module [template renderer routes]
  (module/map->Module {:route (route/route ["/module"])
                       :routes routes
                       :entities nil
                       :name "Text module"
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
   (swap! sys/storage assoc-in [:renderers ::renderer] renderer)
   (swap! sys/storage assoc-in [:templates ::template] template)
   (render/render (get-module ::template ::renderer routes) {:uri "/module"}))
 => "<!DOCTYPE html>\n<html><head><title>Hi!</title></head><body><div>Hi there!</div></body></html>")


(defn raw-http-any [request properties params]
  [:div "Hi there!"])

(fact
 "Simple renderer with just a :render-fn defined and no template"
 (let [routes [(route/route ["/" ^:meta {:name ::index} {:any raw-http-any}])]
       renderer (get-renderer)]
   (swap! sys/storage assoc-in [:renderers ::renderer] renderer)
   (render/render (get-module nil ::renderer routes) {:uri "/module"}))
 => "<div>Hi there!</div>")


(defn raw-http-any-exact [request properties params]
  {:body "raw text"
   :headers {"Content-Type" "plain/text"}
   :status 200})

(fact
 "Simple renderer with just a :render-fn defined and a method that returns exactly what it wants"
 (let [routes [(route/route ["/" ^:meta {:name ::index} {:any raw-http-any-exact}])]
       renderer (get-renderer)]
   (swap! sys/storage assoc-in [:renderers ::renderer] renderer)
   (render/render (get-module nil ::renderer routes) {:uri "/module"}))
 => {:body "raw text"
     :headers {"Content-Type" "plain/text"}
     :status 200})
