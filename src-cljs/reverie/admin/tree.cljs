(ns reverie.admin.tree
  (:require [clojure.string :as s]
            [crate.core :as crate]
            [jayq.core :as jq]
            [jayq.util :as util]
            [reverie.dom :as dom]
            [reverie.meta :as meta]
            [reverie.admin.area :as area]
            [reverie.admin.options :as options]))


(defn- on-drag-start [node]
  (if (false? (-> node .-data .-draggable))
    false
    true))

(defn- on-drag-enter [node source-node]
  true)

(defn- on-drag-over [node source-node hit-mode]
  (if (false? (-> node .-data .-draggable))
    false
    (clj->js ["before" "after"])))

(defn- on-drop [node source-node hit-mode ui draggable]
  (let [source-node-serial (-> source-node .-data .-serial)
        node-serial (-> node .-data .-serial)]
    (jq/xhr [:get (str "/admin/api/pages/move/"
                       node-serial
                       "/"
                       source-node-serial
                       "/"
                       hit-mode)] (clj->js {})
                       (fn [data]
                         (if (.-result data)
                           (do
                             (.expand source-node true)
                             (.move source-node node hit-mode)))))))

(defn- get-active-node []
  (-> :#tree jq/$ (.dynatree "getActiveNode")))

(defn- get-node [serial]
  (-> :#tree jq/$ (.dynatree "getTree") (.getNodeByKey (str serial))))

(defn edit-mode! [e]
  (let [serial (-> (get-active-node) .-data .-serial)]
    (jq/xhr [:get (str "/admin/api/pages/edit/" serial)]
            nil
            (fn [data]
              (if (.-result data)
                (do
                  (-> :.icon-edit-sign jq/$ (jq/add-class "hidden"))
                  (-> :.icon-eye-open jq/$ (jq/remove-class "hidden"))
                  (dom/reload-main!)))))))

(defn view-mode! [e]
  (jq/xhr [:get "/admin/api/pages/view"]
          nil
          (fn [data]
            (if (.-result data)
              (do
                (-> :.icon-eye-open jq/$ (jq/add-class "hidden"))
                (-> :.icon-edit-sign jq/$ (jq/remove-class "hidden"))
                (dom/reload-main!))))))

(defn- load-key-path-cb [node status]
  (case status
    "loaded" (.expand node)
    "ok" (do
           (.activate node)
           (dom/main-uri! (.-data.uri node)))
    "notfound" (util/log "Search: Node not found!")))

(defn- search! [e]
  (let [serial (s/replace (-> :#tree-search jq/$ jq/val) #"\s" "")]
    (if (re-find #"^\d+$" serial)
      (jq/xhr [:post "/admin/api/pages/search"]
              {:serial serial}
              (fn [data]
                (if (.-result data)
                  (-> :#tree
                      jq/$
                      (.dynatree "getTree")
                      (.loadKeyPath (str "/" (s/join "/" (.-path data)))
                                    load-key-path-cb)))))))
  false)

(defn listen! []
  (-> :#tree-search-form
      jq/$
      (jq/on :submit search!))
  (-> :#tree-search-icon
      jq/$
      (jq/on :click search!))
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
      (jq/on :click :.icon-edit-sign nil edit-mode!)
      (jq/on :click :.icon-eye-open nil view-mode!)
      (jq/on :click :.icon-trash nil #(if-let [node (get-active-node)]
                                            (if-let [serial (-> node .-data .-serial)]
                                              (options/delete! serial))))))

(defn- on-lazy-read [node]
  (let [serial (-> node .-data .-serial)]
   (.appendAjax node (clj->js {:url (str "/admin/api/pages/read/" serial)}))))

(defn- on-activation [node]
  (let [data (js->clj (.-data node) :keywordize-keys true)]
    (-> :#tree-search
        jq/$
        (jq/val (:serial data)))
    (meta/display data)
    node))

(defn- on-post-init [reloading? error?]
  (if-not error?
    (.activate (get-node (get-in @meta/data [:pages :root])))))

(defn- get-settings []
  (clj->js
   { :initAjax {:url "/admin/api/pages/read"
                :data {:mode "all"}}
    :debugLevel 0
    :imagePath "../css/dyna-skin/"
    :keyboard false
    :onLazyRead on-lazy-read
    :onActivate on-activation
    :onPostInit on-post-init
    :dnd {
          :onDragStart on-drag-start
          :onDragEnter on-drag-enter
          :onDragOver on-drag-over
          :onDrop on-drop
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

(defn ^:export metad [data]
  (dom/main-uri! (.-uri data))
  (dom/show-main))

(defn ^:export init []
  ;;(util/log (get-settings))
  (-> :#tree
      jq/$
      jq/empty
      (.dynatree (get-settings))))
