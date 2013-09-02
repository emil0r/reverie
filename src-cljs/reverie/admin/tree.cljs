(ns reverie.admin.tree
  (:require [reverie.dom :as dom]
            [reverie.meta :as meta]
            [reverie.admin.options :as options]
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

(defn- get-active-node []
  (-> :#tree jq/$ (.dynatree "getActiveNode")))

(defn- get-node [serial]
  (-> :#tree jq/$ (.dynatree "getTree") (.getNodeByKey (str serial))))

(defn foo [])



(defn listen! []
  (-> :.icons
      jq/$
      (jq/off :click :.icon-refresh)
      (jq/off :click :.icon-plus-sign)
      (jq/off :click :.icon-edit-sign)
      (jq/off :click :.icon-eye-open)
      (jq/off :click :.icon-trash))
  (-> :.icons
      jq/$
      (jq/on :click :.icon-refresh nil #(if-let [node (get-active-node)]
                                          (if-let [uri (-> node .-data .-uri)]
                                            (options/refresh! uri))))
      (jq/on :click :.icon-plus-sign nil #(if-let [node (get-active-node)]
                                            (if-let [serial (-> node .-data .-serial)]
                                              (options/add-page! serial))))
      (jq/on :click :.icon-edit-sign nil foo)
      (jq/on :click :.icon-eye-open nil foo)
      (jq/on :click :.icon-trash nil #(if-let [node (get-active-node)]
                                            (if-let [serial (-> node .-data .-serial)]
                                              (options/delete! serial))))))

(defn on-lazy-read [node]
  (let [serial (-> node .-data .-serial)]
   (.appendAjax node (clj->js {:url (str "/admin/api/pages/read/" serial)}))))

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
    :onLazyRead on-lazy-read
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
  (-> :#tree
      jq/$
      (.dynatree "getTree")
      (.reload)))

(defn ^:export added [data]
  (.addChild (get-active-node) data))

(defn ^:export deleted [data]
  (let [serial (.-serial data)
        node (get-node serial)
        parent (get-node "trash")]
    (set! (-> node .-data .-version) -1)
    (.move node parent "child")))

(defn ^:export restored [data]
  (let [node (get-node (.-serial data))
        parent (get-node (.-parent data))]
    (set! (-> node .-data .-version) 0)
    (.move node parent "child")))

(defn ^:export init []
  ;;(util/log (get-settings))
  (-> :#tree
      jq/$
      jq/empty
      (.dynatree (get-settings))))

