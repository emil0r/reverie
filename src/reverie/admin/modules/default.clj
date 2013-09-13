(ns reverie.admin.modules.default
  (:require [clojure.string :as s]
            [korma.core :as k])
  (:use [reverie.admin.templates :only [frame]]
        [reverie.admin.frames.common :only [frame-options]]
        reverie.admin.modules.helpers
        [reverie.atoms :only [pages modules]]
        reverie.batteries.breadcrumbs
        reverie.batteries.paginator
        [reverie.core :only [defmodule]]))


(defn navbar [{:keys [uri real-uri] :as request}]
  (let [uri (last (s/split real-uri #"^/admin/frame/module"))
        {:keys [module module-name]} (get-module request)
        parts (remove s/blank? (s/split uri #"/"))
        uri-data (map
                  (fn [uri]
                    (cond
                     (re-find #"^\d+$" uri) [uri (get-instance-name
                                                  module
                                                  (second parts)
                                                  (read-string uri))]
                     :else [uri (s/capitalize uri)]))
                  parts)
        {:keys [crumbs]} (crumb uri-data {:base-uri "/admin/frame/module/"})]
    [:nav crumbs]))

(defmodule reverie-default-module {}
  [:get ["/"]
   (let [{:keys [module-name module]} (get-module request)]
     (frame
      frame-options
      [:div.holder
       [:nav "&nbsp;"]
       [:table.table.entities
        (map (fn [e]
               [:tr [:td [:a {:href (str "/admin/frame/module/"
                                         (name module-name)
                                         "/" (name e))}
                          (get-entity-name module e)]]])
             (keys (:entities module)))]]))]
  [:get ["/:entity"]
   (let [{:keys [module-name module]} (get-module request)
         display-fields (get-display-fields module entity)
         fields (get-fields module entity display-fields)
         {:keys [page]} (:params request)
         entities (get-entities module entity page)]
     (frame
      frame-options
      [:div.holder
       (navbar request)
       [:table.table.entity {:id entity}
        [:tr
         (map (fn [f]
                [:th (get-field-name module entity f)])
              display-fields)]
        (map #(get-entity-row % display-fields module-name entity) entities)]]))]

  [:get ["/:entity/:id" {:id #"\d+"}]
   (frame
    frame-options
    [:div.holder
     (navbar request)
     "my form"])]
  )
