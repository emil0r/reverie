(ns reverie.admin.area
  (:require [crate.core :as crate]
            [jayq.core :as jq]
            [jayq.util :as util]
            [reverie.dom :as dom]
            [reverie.meta :as meta]))


(defn- hide-area-menu! []
  (-> :.reverie-area-menu
      dom/$m
      jq/remove))

(defn- hide-object-menu! []
  (-> :.reverie-object-menu
      dom/$m
      jq/remove))

(defn- get-event-object-attr [e attr]
  (-> e .-target jq/$
      jq/parent
      jq/parent
      jq/parent
      jq/parent
      (jq/attr attr)))

(defn- create-area-menu! [$elem]
  (hide-area-menu!)
  (let [objects (:objects @meta/data)]
    (jq/append $elem
               (crate/html
                [:ul.reverie-area-menu
                 [:li.add-objects "Add object"
                  [:ul.reverie-objects
                   (map (fn [o] [:li o]) objects)]]]))))

(defn- create-object-menu! [$elem]
  (hide-object-menu!)
  (jq/append $elem
             (crate/html
              [:ul.reverie-object-menu
               [:li.edit-object {:action "edit"} "Edit"]
               [:li.delete-object {:action "delete"} "Delete"]
               [:li.reverie-bar]
               [:li.move-object-to-top {:action "move-to-top"} "Move to top"]
               [:li.move-object-up {:action "move-up"} "Move up"]
               [:li.move-object-down {:action "move-down"} "Move down"]
               [:li.move-object-to-bottom {:action "move-to-bottom"} "Move to bottom"]])))

(defn- click-area! [e]
  (.stopPropagation e)
  (-> e
      .-target
      jq/$
      jq/parent
      (create-area-menu!)))

(defn- click-area-menu-objects! [e]
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

(defn- click-object! [e]
  (.stopPropagation e)
  (-> e
      .-target
      jq/$
      (create-object-menu!)))

(defmulti click-object-method! (fn [e] (-> e .-target jq/$ (jq/attr :action))))
(defmethod click-object-method! "delete" [e]
  (let [object-id (get-event-object-attr e :object-id)]
    (jq/xhr [:get (str "/admin/api/objects/delete/" object-id)]
            nil
            (fn [data]
              (if (.-result data)
                (dom/reload-main!))))))
(defmethod click-object-method! :default [e]
  (js/alert "No method defined"))

(defn listen! []
  (-> (dom/$m-html)
      (jq/off :click hide-area-menu!)
      (jq/off :click hide-object-menu!))
  (-> (dom/$m-html)
      (jq/on :click hide-area-menu!)
      (jq/on :click hide-object-menu!))

  (-> :.reverie-area
      dom/$m
      (jq/off :.reverie-area-panel)
      (jq/off :.reverie-area-menu)
      (jq/off :.reverie-object-panel)
      (jq/off :.reverie-object-menu)
      (jq/off ".reverie-objects>li")
      (jq/off ".reverie-object-menu>li"))
  (-> :.reverie-area
      dom/$m
      (jq/on :click :.reverie-area-panel nil click-area!)
      (jq/delegate ".reverie-objects>li" :click click-area-menu-objects!)
      (jq/delegate :.reverie-object-panel :click click-object!)
      (jq/delegate ".reverie-object-menu>li" :click click-object-method!)))
