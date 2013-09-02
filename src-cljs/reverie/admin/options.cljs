(ns reverie.admin.options
  (:require [jayq.core :as jq]
            [jayq.util :as util]
            [reverie.dom :as dom]))


(defn- get-serial [e]
  (-> e .-target jq/$ (jq/attr :serial)))

(defn new-root-page! []
  (dom/show-options)
  (dom/options-uri! "/admin/frame/options/new-root-page"))


(defn publish-page! [e]
  (dom/options-uri! (str "/admin/frame/options/publish/" (get-serial e)))
  (dom/show-options))

(defn meta-page! [e]
  (dom/show-options)
  (dom/options-uri! (str "/admin/frame/options/meta?serial=" (get-serial e))))

(defn add-page! [serial]
  (dom/options-uri! (str "/admin/frame/options/add-page?serial=" serial))
  (dom/show-options))

(defn refresh! [uri]
  (dom/main-uri! uri)
  (dom/show-main))

(defn restore! [e]
  (dom/options-uri! (str "/admin/frame/options/restore?serial=" (get-serial e)))
  (dom/show-options))

(defn delete! [serial]
  (dom/options-uri! (str "/admin/frame/options/delete?serial=" serial))
  (dom/show-options))
