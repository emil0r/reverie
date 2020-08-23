(ns reverie.config
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]))

(defonce config (atom {}))

(defrecord ConfigManager [started? config-settings]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (log/info "Starting ConfigManager")
          (log/info "Resetting config/config")
          (reset! config config-settings)
          (assoc this
                 :started? true))))
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping ConfigManager")
          (reset! config {})
          (assoc this
                 :started? false)))))

(defn config-manager [settings]
  (map->ConfigManager {:config-settings settings}))
