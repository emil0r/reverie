(ns reverie.meta
  (:require [crate.core :as crate]
            [jayq.core :as jq]
            [jayq.util :as util]
            [reverie.admin.options :as options]
            [reverie.util :as util2]))

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
      (jq/off :click :.edit)
      (jq/off :click :.publish))
  (-> :.meta
      jq/$
      (jq/on :click :.publish nil options/publish-page!)
      (jq/on :click :.edit nil options/edit-page!)))

(defn display [data]
  (-> :.meta
      jq/$
      (jq/html (crate/html [:div
                            [:table.meta
                             [:tr [:th "Name"] [:td (:title data)]]
                             [:tr [:th "Title"] [:td (:real-title data)]]
                             [:tr [:th "Created"] [:td (util2/date-format (:created data))]]
                             [:tr [:th "Updated"] [:td (util2/date-format (:updated data))]]]
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
