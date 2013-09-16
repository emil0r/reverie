(ns reverie.admin.file-picker
  "This namespace is only run in the frame for the file picker"
  (:require [jayq.core :as jq]
            [jayq.util :as util])
  (:use [reverie.util :only [query-params join-uri]]))


(defn set-file! [e]
  (let [url (-> e .-target jq/$ (jq/attr :uri))
        params (query-params)]
    (set! (-> js/opener
              .-document
              (.getElementById (:field-name params))
              .-value) url))
  (.close js/window))

(defn init []
  (let [params (query-params)]
    (-> (str "span[uri='" (:value params) "']")
        jq/$
        (jq/add-class :selected)))
  (-> :span.download
      jq/$
      (jq/on :click set-file!)))
