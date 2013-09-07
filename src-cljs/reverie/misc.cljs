(ns reverie.misc
  (:require [jayq.core :as jq]
            [jayq.util :as util]))


(defn logout! [e]
  (.stopPropagation e)
  (set! (-> js/window
            .-parent
            .-document
            .-location
            .-pathname)
        "/admin/logout")
  false)

(defn listen! []
  (-> "div.logout a"
      jq/$
      (jq/on :click logout!)))
