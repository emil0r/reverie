(ns reverie.logger
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.rotor :refer [rotor-appender]]))


(defrecord Logger [prod? rotor initialized?]
  component/Lifecycle
  (start [this]
    (if initialized?
      this
      (do
        (when prod?
          (when rotor
            (log/set-config! [:appenders :rotor]
                             (merge rotor-appender rotor))
            (log/set-config! [:shared-appender-config :rotor]
                             (merge rotor-appender rotor))))
        (log/info "Starting logging")
        (if (and prod? rotor)
          (log/info "Starting log at:" (:path rotor)))
        (assoc this
          :initialized? true))))
  (stop [this]
    (if-not initialized?
      this
      (do
        (log/info "Stopping logging")
        (assoc this
          :initialized? nil)))))


(defn logger [prod? rotor]
  (map->Logger {:prod? prod? :rotor rotor}))
