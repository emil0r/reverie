(ns reverie.admin.area
  (:require [crate.core :as crate]
            [jayq.core :as jq]
            [jayq.util :as util]
            [reverie.dom :as dom]
            [reverie.meta :as meta])
  (:use [reverie.util :only [ev$]]))

(def areas (atom #{}))

(defn- hide-area-menu! []
  (-> :.reverie-area-menu
      dom/$m
      jq/remove))

(defn- hide-object-menu! []
  (-> :.reverie-object-menu
      dom/$m
      jq/remove))

(defn- paste? []
  (or (-> @meta/data :edits :object :cut)
      (-> @meta/data :edits :object :copy)))

(defn- create-area-menu! [$elem]
  (hide-area-menu!)
  (let [area (jq/attr $elem :area)
        objects (:objects @meta/data)]
    (jq/append $elem
               (crate/html
                [:ul.reverie-area-menu
                 [:li.add-objects "Add object"
                  [:ul.reverie-objects
                   (map (fn [o] [:li o]) objects)]]
                 (if (paste?)
                   (list
                    [:li.reverie-bar]
                    [:li.paste-object {:action "paste" :area area :type "area"} "Paste"]))]))))

(defn- create-object-menu! [$elem {:keys [area]}]
  (hide-object-menu!)
  (jq/append $elem
             (crate/html
              [:ul.reverie-object-menu
               [:li.edit-object {:action "edit"} "Edit"]
               [:li.delete-object {:action "delete"} "Delete"]
               (if (paste?)
                 (list
                  [:li.reverie-bar]
                  [:li.paste-object {:action "paste" :area area :type "object"} "Paste"]))
               [:li.reverie-bar]
               [:li.copy-object {:action "cut"} "Cut"]
               [:li.copy-object {:action "copy"} "Copy"]
               [:li.move-object "Move to »"
                [:ul.move-object-to
                 (map (fn [a] [:li {:action "move-to-area" :area a} "area " a])
                      (remove #(= area %) @areas))
                 [:li.reverie-bar]
                 [:li.move-object-to-top {:action "move-to-top"} "Move to top"]
                 [:li.move-object-up {:action "move-up"} "Move up"]
                 [:li.move-object-down {:action "move-down"} "Move down"]
                 [:li.move-object-to-bottom {:action "move-to-bottom"} "Move to bottom"]]]])))

(defn- click-area! [e]
  (.stopPropagation e)
  (-> e ev$
      (jq/parents :.reverie-area)
      (create-area-menu!)))

(defn- click-area-menu-objects! [e]
  (.stopPropagation e)
  (let [$e (ev$ e)
        object (jq/html $e)
        area (-> $e (jq/parents :.reverie-area) (jq/attr :area))
        serial (-> $e (jq/parents :.reverie-area) (jq/attr :page-serial))]
    (jq/xhr [:post (str "/admin/api/objects/add/"
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
  (-> e ev$
      (create-object-menu! {:area (-> e ev$
                                      (jq/parents :.reverie-area)
                                      (jq/attr :area))})))

(defmulti click-object-method! (fn [e] (-> e .-target jq/$ (jq/attr :action))))
(defmethod click-object-method! "delete" [e]
  (let [object-id (-> e ev$ (jq/parents :.reverie-object) (jq/attr :object-id))]
    (jq/xhr [:post "/admin/api/objects/delete/"]
            {:object-id object-id}
            (fn [data]
              (if (.-result data)
                (dom/reload-main!))))))
(defmethod click-object-method! "edit" [e]
  (let [object-id (-> e ev$ (jq/parents :.reverie-object) (jq/attr :object-id))]
    (.open js/window
           (str "/admin/frame/object/edit?object-id=" object-id)
           "_blank"
           "height=640,width=400,location=0,menubar=0,resizable=1,scrollbars=1,status=0,titlebar=1"
           true)))
(defmethod click-object-method! "move-to-area" [e]
  (let [$e (ev$ e)
        object-id (-> $e (jq/parents :.reverie-object) (jq/attr :object-id))
        area (-> $e (jq/attr :area))]
    (jq/xhr [:post "/admin/api/objects/move/area"]
            {:anchor area :object-id object-id :hit-mode "area"}
            (fn [data]
              (if (.-result data)
                (dom/reload-main!))))))
(defmethod click-object-method! "move-to-top" [e]
  (let [$e (ev$ e)
        object-id (-> $e (jq/parents :.reverie-object) (jq/attr :object-id))]
    (jq/xhr [:post "/admin/api/objects/move"]
            {:object-id object-id :hit-mode "top"}
            (fn [data]
              (if (.-result data)
                (dom/reload-main!))))))
(defmethod click-object-method! "move-to-bottom" [e]
  (let [$e (ev$ e)
        object-id (-> $e (jq/parents :.reverie-object) (jq/attr :object-id))]
    (jq/xhr [:post "/admin/api/objects/move"]
            {:object-id object-id :hit-mode "bottom"}
            (fn [data]
              (if (.-result data)
                (dom/reload-main!))))))
(defmethod click-object-method! "move-up" [e]
  (let [$e (ev$ e)
        object-id (-> $e (jq/parents :.reverie-object) (jq/attr :object-id))]
    (jq/xhr [:post "/admin/api/objects/move"]
            {:object-id object-id :hit-mode "up"}
            (fn [data]
              (if (.-result data)
                (dom/reload-main!))))))
(defmethod click-object-method! "move-down" [e]
  (let [$e (ev$ e)
        object-id (-> $e (jq/parents :.reverie-object) (jq/attr :object-id))]
    (jq/xhr [:post "/admin/api/objects/move"]
            {:object-id object-id :hit-mode "down"}
            (fn [data]
              (if (.-result data)
                (dom/reload-main!))))))
(defmethod click-object-method! "cut" [e]
  (let [$e (ev$ e)
        object-id (-> $e (jq/parents :.reverie-object) (jq/attr :object-id))]
    (jq/xhr [:post "/admin/api/objects/cut"]
            {:object-id object-id}
            (fn [data]
              (if (.-result data)
                (do
                  (meta/sync!)
                  (doseq [obj (-> :.reverie-object dom/$m)]
                    (jq/remove-class (jq/$ obj) :reverie-ready-for-copy)
                    (jq/remove-class (jq/$ obj) :reverie-ready-for-cut))
                  (-> (str "div[object-id='" object-id "']")
                      dom/$m
                      (jq/add-class :reverie-ready-for-cut))))))))
(defmethod click-object-method! "copy" [e]
  (let [$e (ev$ e)
        object-id (-> $e (jq/parents :.reverie-object) (jq/attr :object-id))]
    (jq/xhr [:post "/admin/api/objects/copy"]
            {:object-id object-id}
            (fn [data]
              (if (.-result data)
                (do
                  (meta/sync!)
                  (doseq [obj (-> :.reverie-object dom/$m)]
                    (jq/remove-class (jq/$ obj) :reverie-ready-for-copy)
                    (jq/remove-class (jq/$ obj) :reverie-ready-for-cut))
                  (-> (str "div[object-id='" object-id "']")
                      dom/$m
                      (jq/add-class :reverie-ready-for-copy))))))))
(defmethod click-object-method! "paste" [e]
  (let [$e (ev$ e)
        object-id (or (-> @meta/data :edits :object :cut)
                      (-> @meta/data :edits :object :copy))
        area (jq/attr $e :area)
        type (jq/attr $e :type)
        data (merge {:area area
                     :type type}
                    (case type
                      "object" {:object-id object-id}
                      "area" {:page-serial (get-in @meta/data [:pages :current])}
                      {}))]
    (jq/xhr [:post "/admin/api/objects/paste"]
            data
            (fn [data]
              (if (.-result data)
                (do
                  (meta/sync!)
                  (dom/reload-main!)))))))
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

(defn init []
  (doseq [area (-> :.reverie-area dom/$m)]
    (swap! areas conj (-> area jq/$ (jq/attr :area))))
  (listen!))
