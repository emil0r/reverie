(ns reverie.admin.options.page
  (:require [clojure.string :as s]
            [crate.core :as crate]
            [jayq.core :as jq]
            [jayq.util :as util]
            [reverie.meta :as meta])
  (:use [cljs.reader :only [read-string]]
        [reverie.util :only [normalize]]))

(defn- get-template-areas [template]
  (get-in @meta/data [:templates (keyword template) :options :template/areas]))

(defn- get-app-areas [app]
  (get-in @meta/data [:apps (keyword app) :options :app/areas]))

(defn- set-area-mappings! []
  (-> :#app_template_bindings
      jq/$
      (jq/val
       (pr-str
        (into
         {}
         (map (fn [m]
                (let [$m (jq/$ m)
                      template-area (-> $m (jq/attr :id) (s/replace #"^template-area-" "") keyword)
                      app-area (-> $m jq/val keyword)]
                  {template-area app-area}))
              (-> "#area-mapping-holder select" jq/$)))))))

(defn- print-area-mappings []
  (let [current-template (-> :#template jq/$ jq/val keyword)
        current-app (-> :#app jq/$ jq/val keyword)
        template-areas (get-template-areas current-template)
        app-areas (get-app-areas current-app)
        current-mappings (-> :#app_template_bindings jq/$ jq/val read-string)]
    (-> :#area-mapping-holder jq/$ jq/empty)
    (-> :#area-mapping-holder jq/$
        (jq/append
         (crate/html
          [:table.area-mapping
           [:tr [:th "Template area"] [:th "App area"]]
           (map (fn [t]
                  (let [n (str "template-area-" t)
                        area-selected (get current-mappings t)]
                    [:tr
                     [:td t]
                     [:td [:select {:id n :class "template-area"}
                           (map (fn [a]
                                  [:option {:selected (= a area-selected)}
                                   a])
                                app-areas)]]]))
                template-areas)])))
    (-> :tr.areas jq/$ (jq/remove-class "hidden"))
    (set-area-mappings!)))

(defn switch-template-app [e]
  (case (-> :#type jq/$ jq/val)
    "app" (do
            (-> :tr.app jq/$ (jq/remove-class "hidden"))
            (print-area-mappings))
    (do
      (-> :tr.app jq/$ (jq/add-class "hidden"))
      (-> :tr.areas jq/$ (jq/add-class "hidden")))))

(defn change-uri []
  (let [name (-> :#name jq/$ jq/val)]
    (-> :#uri jq/$ (jq/val (normalize name)))))

(defn init-templates-app []
  (if (= "app" (-> :#type jq/$ jq/val))
    (switch-template-app nil)))

(defn init []
  (meta/read!
   #(do
      (init-templates-app)
      (-> :#type jq/$ (jq/bind :change switch-template-app))
      (-> :#name jq/$ (jq/bind :change change-uri))
      (-> :#area-mapping-holder jq/$
          (jq/delegate :.template-area :change set-area-mappings!)))))
