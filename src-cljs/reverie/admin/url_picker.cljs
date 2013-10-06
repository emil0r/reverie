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

(defn changed! [e]
  (let [value (-> e ev$ jq/val)]
    (-> :#value jq/$ (jq/val value))))

(defn save! [e]
  (let [params (query-params)
        value (-> :#value jq/$ jq/val)]
    (set! (-> js/opener
              .-document
              (.getElementById (:field-name params))
              .-value) value)
    (.close js/window)))

(defn init []
  (let [params (query-params)
        tb (goog.ui/TabBar.)]
    (.decorate tb (dom/getElement "tabbar"))
    (events/listen tb goog.ui.Component.EventType.SELECT click-tab!))
  (-> :#freestyle
      jq/$
      (jq/on :change changed!))
  (-> :#page-dropdown
      jq/$
      (jq/on :change changed!))
  (-> :#save
      jq/$
      (jq/on :click save!)))


(defn set-file! [e]
  (let [url (-> e .-target jq/$ (jq/attr :uri))
        params (query-params)]
    (set! (-> js/parent
              .-document
              (.getElementById (:field-name params))
              .-value) url)
    (-> :.selected jq/$ (jq/remove-class :selected))
    (-> e ev$ (jq/add-class :selected))))

(defn init-files []
  (let [params (query-params)]
    (-> (str "span[uri='" (:value params) "']")
        jq/$
        (jq/add-class :selected))
    (-> :span.download
        jq/$
        (jq/on :click set-file!))))
