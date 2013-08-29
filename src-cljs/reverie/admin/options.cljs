(ns reverie.admin.options
  (:require [jayq.core :as jq]
            [reverie.dom :as dom]))


(defn new-root-page! []
  (dom/show-options)
  (dom/options-uri! "/admin/frame/options/new-root-page"))
