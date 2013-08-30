(ns reverie.meta
  (:require [crate.core :as crate]
            [jayq.core :as jq]
            [jayq.util :as util]
            [reverie.admin.options :as options]))

(def data (atom {}))

(defn read! [f]
  (jq/xhr [:get "/admin/api/meta"] {}
          (fn [json-data]
            (let [edn-data (js->clj json-data :keywordize-keys true)]
              (reset! data edn-data)
              (f)))))


(defn listen! []
  (-> :.meta
      jq/$
      (jq/off :click :.edit))
  (-> :.meta
      jq/$
      (jq/off :click :.publish))
  (-> :.meta
      jq/$
      (jq/on :click :.edit nil options/edit-page!))
  (-> :.meta
      jq/$
      (jq/on :click :.publish nil options/publish-page!)))

(defn display [data]
  (-> :.meta
      jq/$
      (jq/html (crate/html [:div
                            [:table.meta
                             [:tr [:th "Name"] [:td (:title data)]]
                             [:tr [:th "Title"] [:td (:real-title data)]]
                             [:tr [:th "Created"] [:td (:created data)]]
                             [:tr [:th "Updated"] [:td (:updated data)]]]
                            [:div.buttons
                             [:div.btn.btn-primary.publish
                              {:serial (:serial data)
                               :page-id (:id data)}
                              (if (:published? data)
                                "Unpublish"
                                "Publish")]
                             [:div.btn.btn-primary.edit
                              {:serial (:serial data)
                               :page-id (:id data)}
                              "Edit"]]]))))
