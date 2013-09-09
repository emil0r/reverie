(ns reverie.admin.filemanager
  "This namespace is only run in the frame for the file manager"
  (:require [jayq.core :as jq]
            [jayq.util :as util])
  (:use [reverie.util :only [query-params]]))



(defn set-file! [filename]
  (set! (-> js/opener
            .-document
            (.getElementById (:field-name params))
            .-value) filename))

(defn init []
  (let [params (query-params)]))
