(ns reverie.admin.tabs
  (:require [goog.events :as events]
            [goog.ui.TabBar :as tb]
            [goog.ui.Tab :as t]
            [goog.dom :as dom]
            [jayq.core :as jq]
            [jayq.util :as util]))



(defn click-tab! [e]
  (let [tab (-> e
                .-target
                .-element_
                jq/$
                (jq/attr :tab))]
    (if (= tab "navigation-meta")
      (do
        (-> :.modules
            jq/$
            (jq/add-class :hidden))
        (-> :.navigation-meta
            jq/$
            (jq/remove-class :hidden)))
      ;; else
      (do
        (-> :.modules
            jq/$
            (jq/remove-class :hidden))
        (-> :.navigation-meta
            jq/$
            (jq/add-class :hidden))))))

(defn init []
  (let [tb (goog.ui/TabBar.)]
    (.decorate tb (dom/getElement "tabbar"))
    (events/listen tb goog.ui.Component.EventType.SELECT click-tab!)))
