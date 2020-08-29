(ns reverie.core
  (:require [reagent.dom :as dom]
            [reverie.init :as init]
            [reverie.views :as views]))


(defn start []
  (dom/render [views/index]
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


