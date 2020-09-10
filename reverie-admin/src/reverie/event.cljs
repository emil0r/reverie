(ns reverie.event
  (:require [com.stuartsierra.component :as component]
            [reverie.event.auth]
            [reverie.event.init :as event.init]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]))


;; db: we shouldn't get a nil value as dispatch. log it as an error and fix
(rf/reg-event-fx nil (fn [_ & args]
                       (log/error ::nil-event args)
                       nil))

(defrecord EventManager [started?]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (log/info "Starting EventManager")
          (rf/dispatch-sync [::event.init/initialize])
          (assoc this
                 :started? true))))
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping EventManager")
          (assoc this
                 :started? false)))))

(defn event-manager [settings]
  (map->EventManager settings))
