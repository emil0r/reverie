(ns reverie.admin.options.page
  (:require [jayq.core :as jq]
            [jayq.util :as util]))


(defn switch-template-app [e]
  (-> :tr.template jq/$ (jq/toggle-class "hidden"))
  (-> :tr.app jq/$ (jq/toggle-class "hidden")))

(defn init-templates-app []
  (if (= "app" (-> :#type jq/$ jq/val))
    (switch-template-app nil)))

(defn init []
  (util/log "running init")
  (init-templates-app)
  (jq/bind (jq/$ :#type) :change switch-template-app))
