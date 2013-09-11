(ns reverie.admin.tabs
  (:require [goog.events :as events]
            [goog.ui.TabBar :as tb]
            [goog.ui.Tab :as t]
            [goog.dom :as dom]
            [jayq.core :as jq]
            [jayq.util :as util]
            [reverie.dom :as dom2])
  (:use [reverie.util :only [ev$ activate!]]))


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
            (jq/remove-class :hidden))
        (doseq [m (-> ".modules > ul" jq/$ jq/children)]
          (-> m jq/$ (jq/remove-class :active))))
      ;; else
      (do
        (-> :.modules
            jq/$
            (jq/remove-class :hidden))
        (-> :.navigation-meta
            jq/$
            (jq/add-class :hidden))))))

(defn click-module! [e]
  (let [e$ (-> e ev$)
        module (jq/attr e$ :module)]
    (activate! e$)
    
    (dom2/options-uri! (str "/admin/frame/module/" module))
    (dom2/show-options)))

(defn listen! []
  (-> :.modules
      jq/$
      (jq/on :click "ul>li" click-module!)))

(defn init []
  (let [tb (goog.ui/TabBar.)]
    (.decorate tb (dom/getElement "tabbar"))
    (events/listen tb goog.ui.Component.EventType.SELECT click-tab!)
    (listen!)))
