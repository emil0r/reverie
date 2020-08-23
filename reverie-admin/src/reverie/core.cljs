(ns reverie.core
  (:require [reagent.dom :as dom]
            [reverie.init :as init]))


(defn index []
  [:h1 "hi"])

(defn start []
  (dom/render [index]
              (. js/document (getElementById "app"))))


(defn ^:export init [config]
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds
  (init/init (js->clj config :keywordize-keys true))
  (start))

(defn stop []
  ;; stop is called before any code is reloaded
  ;; this is controlled by :before-load in the config
  (js/console.log "stop"))


