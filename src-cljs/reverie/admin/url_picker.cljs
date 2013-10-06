(ns reverie.admin.url-picker
  "This namespace is only run in the frame for the url picker"
  (:require [goog.events :as events]
            [goog.ui.TabBar :as tb]
            [goog.ui.Tab :as t]
            [goog.dom :as dom]
            [jayq.core :as jq]
            [jayq.util :as util]
            [reverie.dom :as dom2])
  (:use [reverie.util :only [query-params join-uri ev$]]))

(defn click-tab! [e]
  (let [tab (-> e
                .-target
                .-element_
                jq/$
                (jq/attr :tab))]
    (doseq [t (-> :.tab jq/$)]
      (-> t jq/$ (jq/add-class :hidden)))
    (-> (str "#" tab) jq/$ (jq/remove-class :hidden))))

(defn init []
  (let [tb (goog.ui/TabBar.)]
    (.decorate tb (dom/getElement "tabbar"))
    (events/listen tb goog.ui.Component.EventType.SELECT click-tab!)))


(defn init-files []
  (util/log "wohoo!"))
