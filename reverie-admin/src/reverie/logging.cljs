(ns reverie.logging
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]))

(defrecord LoggingManager [started?]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (log/info "Starting LoggingManager")
          (if goog/DEBUG
            (do (log/info "Setting log level to :debug")
                (log/set-level! :debug))
            (do (log/info "Setting log level to :info")
                (log/set-level! :info)))
          (assoc this
                 :started? true))))
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping LoggingManager")
          (assoc this
                 :started? false)))))

(defn logging-manager [settings]
  (map->LoggingManager settings))
