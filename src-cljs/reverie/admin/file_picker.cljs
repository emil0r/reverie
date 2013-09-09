(ns reverie.admin.file-picker
  "This namespace is only run in the frame for the file picker"
  (:require [jayq.core :as jq]
            [jayq.util :as util])
  (:use [reverie.util :only [query-params]]))


(defn set-file! [e]
  (let [url (-> e .-target jq/$ (jq/attr :url))
        params (query-params)]
   (set! (-> js/opener
             .-document
             (.getElementById (:field-name params))
             .-value) url))
  (.close js/window))

(defn init []
  (-> :span.download
      jq/$
      (jq/on :click set-file!)))
