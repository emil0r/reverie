(ns reverie.admin.area
  (:require [crate.core :as crate]
            [jayq.core :as jq]
            [jayq.util :as util]
            [reverie.dom :as dom]))


(defn- create-menu! [$elem]
  (jq/append $elem
           (crate/html
            [:ul.reverie-area-menu
             [:li "testus"]
             [:li "foo"]])))

(defn hide-menu! [e]
  (-> :.reverie-area-menu
      dom/$m
      jq/remove))

(defn click-area! [e]
  (.stopPropagation e)
  (-> e
      .-target
      jq/$
      jq/parent
      create-menu!))

(defn listen! []
  (-> (dom/$m-html)
      (jq/off :click hide-menu!))
  (-> (dom/$m-html)
      (jq/on :click nil hide-menu!))
  
  (-> :.reverie-area-panel
      dom/$m
      (jq/off :click))
  (-> :.reverie-area-panel
      dom/$m
      (jq/on :click nil click-area!)))
