(ns reverie.logger
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [taoensso.timbre.appenders.postal :refer [postal-appender]]
            [taoensso.timbre.appenders.3rd-party.rotor :refer [rotor-appender]]))


(def get-appender nil)
(defmulti get-appender (fn [[k v]] k))
(defmethod get-appender :rotor [[k v]]
  [k (rotor-appender v)])
(defmethod get-appender :postal [[k {:keys [from to settings]}]]
  [k (postal-appender
      (with-meta
        {:from from :to to}
        settings))])
(defmethod get-appender :default [_])

(defn get-appenders [appenders]
  (->> appenders
       (map get-appender)
       (remove nil?)
       (into {})))

(defrecord Logger [prod? started? appenders]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do
        (when (and prod? appenders)
          (log/merge-config! {:appenders (get-appenders appenders)}))
        (log/info "Starting logging")
        (assoc this
          :started? true))))
  (stop [this]
    (if-not started?
      this
      (do
        (log/info "Stopping logging")
        (assoc this
          :started? false)))))


(defn logger [prod? appenders]
  (map->Logger {:prod? prod? :appenders appenders}))
