(ns reverie.admin.tree
  (:require [reverie.dom :as dom]
            [reverie.meta :as meta]
            [jayq.core :as jq]
            [jayq.util :as util]
            [crate.core :as crate]))



(defn- on-drag-start [node]
  (util/log "on-drag-start" node)
  true)


(defn- on-drag-stop [node]
  (util/log "on-drag-stop" node)
  )

(defn- on-drag-enter [node source-node]
  (util/log "on-drag-enter" node source-node)
  true)

(defn- on-drag-over [node source-node hit-mode]
  ;;(util/log "on-drag-over" node source-node hit-mode)
  "after")

(defn- on-drop [node source-node hit-mode ui draggable]
  ;;(util/log "on-drop" node source-node hit-mode ui draggable)
  (.move source-node node hit-mode ))

(defn- on-drag-leave [node source-node]
  ;;(util/log "on-drag-leave" node source-node)
  )

(defn listen! []
  (-> :.icons
      jq/$
      (jq/off :click :.icon-refresh)
      (jq/off :click :.icon-plus-sign)
      (jq/off :click :.icon-edit-sign)
      (jq/off :click :.icon-eye-open)
      (jq/off :click :.icon-trash))
  (-> :.meta
      jq/$
      (jq/on :click :.edit nil options/edit-page!))
  (-> :.meta
      jq/$
      (jq/on :click :.publish nil options/publish-page!)))

(defn on-activation [e]
  (let [data (js->clj (.-data e) :keywordize-keys true)]
    (meta/display data)))

(defn- get-settings []
  (clj->js
   { :initAjax {:url "/admin/api/pages/read"
                :data {:mode "all"}}
    :debugLevel 0
    :imagePath "../css/dyna-skin/"
    :keyboard false
    :onActivate on-activation
    :dnd {
          :onDragStart on-drag-start
          :onDragStop on-drag-stop
          :onDragEnter on-drag-enter
          :onDragOver on-drag-over
          :onDrop on-drop
          :onDragLeave on-drag-leave
          :autoExpandMS 1000
          :preventVoidMoves true
          }
    }))

(defn ^:export reload []
  (util/log "Reload tree...")
  (util/log (-> :#tree jq/$))
  (-> :#tree
      jq/$
      (.dynatree "reload")))


(defn ^:export init []
  (util/log (get-settings))
  (-> :#tree
      jq/$
      jq/empty
      (.dynatree (get-settings))))

