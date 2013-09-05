(ns reverie.admin.area
  (:require [crate.core :as crate]
            [jayq.core :as jq]
            [jayq.util :as util]
            [reverie.dom :as dom]
            [reverie.meta :as meta]))


(defn- hide-menu! [e]
  (-> :.reverie-area-menu
      dom/$m
      jq/remove))

(defn- create-menu! [$elem]
  (hide-menu! nil)
  (let [objects (:objects @meta/data)]
    (jq/append $elem
               (crate/html
                [:ul.reverie-area-menu
                 [:li.add-objects "Add object"
                  [:ul.reverie-objects
                   (map (fn [o] [:li o]) objects)]]]))))

(defn- click-area! [e]
  (.stopPropagation e)
  (-> e
      .-target
      jq/$
      jq/parent
      (create-menu!)))

(defn- click-menu-objects! [e]
  (.stopPropagation e)
  (let [$e (-> e .-target jq/$)
        object (jq/html $e)
        area (-> $e jq/parent jq/parent jq/parent jq/parent jq/parent (jq/attr :area))
        serial (-> $e jq/parent jq/parent jq/parent jq/parent jq/parent (jq/attr :page-serial))]
    (jq/xhr [:get (str "/admin/api/objects/add/"
                       serial
                       "/"
                       area
                       "/"
                       object)]
            nil
            (fn [data]
              (dom/reload-main!)))))

(defn listen! []
  (util/log "listening on areas!")
  (-> (dom/$m-html)
      (jq/off :click hide-menu!))
  (-> (dom/$m-html)
      (jq/on :click nil hide-menu!))

  (-> :.reverie-area
      dom/$m
      (jq/off :.reverie-area-panel :click)
      (jq/off :.reverie-area-menu :click))
  (-> :.reverie-area
      dom/$m
      (jq/on :click :.reverie-area-panel nil click-area!)
      (jq/delegate ".reverie-objects>li" :click click-menu-objects!)))
