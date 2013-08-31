(ns reverie.admin.options
  (:require [jayq.core :as jq]
            [jayq.util :as util]
            [reverie.dom :as dom]))


(defn new-root-page! []
  (dom/show-options)
  (dom/options-uri! "/admin/frame/options/new-root-page"))


(defn publish-page! [e]
  (let [serial (-> e .-target jq/$ (jq/attr :serial))]
    (util/log "serial->" serial)
    (dom/options-uri! (str "/admin/frame/options/publish/" serial))
    (dom/show-options)))

(defn edit-page! [e]
  ;;(util/log e)
  (dom/show-options)
  ;;(dom/options-uri! "/admin/frame/options/edit")
  )

(defn add-page! [serial]
  (dom/options-uri! (str "/admin/frame/options/add-page?serial=" serial))
  (dom/show-options))

(defn refresh! [uri]
  (dom/main-uri uri)
  (dom/show-main))
