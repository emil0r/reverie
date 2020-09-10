(ns reverie.init
  (:require [com.stuartsierra.component :as component]
            [reverie.communication :as communication]
            [reverie.config :refer [config]]
            [reverie.event :as event]
            [reverie.i18n :as i18n]
            [reverie.logging :as logging]
            [reverie.subs]
            [taoensso.timbre :as log]))

(defonce -system (atom nil))

(defrecord InitManager [started?]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (log/info "Starting InitManager")
          (assoc this
                 :started? this))))
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping InitManager")
          (assoc this
                 :started? false)))))


(defn init-manager [settings]
  (map->InitManager settings))


(defn- system-map [config-settings]
  (let [config-manager (component/start (reverie.config/config-manager config-settings))]
    (component/system-map
     :config-manager config-manager

     :init          (component/using
                     (init-manager (:init @config))
                     [])
     :logging-manager (component/using
                       (logging/logging-manager (:logging-manager @config))
                       [])
     :event-manager (component/using
                     (event/event-manager (:event-manager @config))
                     [])
     :communication-manager (component/using
                             (communication/communication-manager (:communication-manager @config))
                             []))))

(defn init [config-settings]
  (log/info "Initializing reverie admin")
  (reset! -system (component/start (system-map config-settings))))

