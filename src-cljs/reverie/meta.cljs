(ns reverie.meta
  (:require [crate.core :as crate]
            [jayq.core :as jq]
            [jayq.util :as util]
            [reverie.admin.options :as options]
            [reverie.util :as util2]))

(def data (atom {}))

(defn sync! []
  (jq/xhr [:get "/admin/api/meta"] {}
          (fn [json-data]
            (let [edn-data (js->clj json-data :keywordize-keys true)]
              (reset! data edn-data)))))

(defn read! [f]
  (jq/xhr [:get "/admin/api/meta"] {}
          (fn [json-data]
            (let [edn-data (js->clj json-data :keywordize-keys true)]
              (reset! data edn-data)
              (f)))))


(defn listen! []
  (-> :.meta
      jq/$
      (jq/off :click :.publish)
      (jq/off :click :.meta)
      (jq/off :click :.restore))
  (-> :.meta
      jq/$
      (jq/on :click :.publish nil options/publish-page!)
      (jq/on :click :.meta nil options/meta-page!)
      (jq/on :click :.restore nil options/restore!)))

(defn display [data]
  (case (:version data)
    "trash" (-> :.meta
                jq/$
                jq/empty)
    -1 (-> :.meta
             jq/$
             (jq/html (crate/html
                       [:div.buttons
                        [:div.btn.btn-primary.restore
                         {:serial (:serial data)}
                         "Restore"]])))
    (-> :.meta
        jq/$
        (jq/html (crate/html [:div
                              [:table.meta
                               [:tr [:th "Name"] [:td (:title data)]]
                               [:tr [:th "Title"] [:td (:real-title data)]]
                               [:tr [:th "Created"] [:td (util2/date-format (:created data))]]
                               [:tr [:th "Updated"] [:td (util2/date-format (:updated data))]]
                               [:tr [:th "Published?"] [:td (:published? data)]]]
                              [:div.buttons
                               [:div.btn.btn-primary.publish
                                {:serial (:serial data)
                                 :page-id (:id data)}
                                "Publishing"]
                               [:div.btn.btn-primary.meta
                                {:serial (:serial data)
                                 :page-id (:id data)}
                                "Meta"]]])))
    ))
