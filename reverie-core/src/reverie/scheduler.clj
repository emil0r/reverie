(ns reverie.scheduler
  (:require [com.stuartsierra.component :as component]
            [cronj.core :as cron]
            [taoensso.timbre :as log]))


(defrecord Task [id desc handler pre-hook post-hook schedule opts])

;; cj stands for cronjobs
(defrecord Scheduler [tasks cj]
  component/Lifecycle
  (start [this]
    (log/info "Starting scheduler")
    (if cj
      this
      (let [cj (cron/cronj :entries tasks)]
        (cron/start! cj)
        (assoc this
          :cj cj))))
  (stop [this]
    (log/info "Stopping scheduler")
    (if-not cj
      this
      (do
        (cron/stop! cj)
        (assoc this
          :cj nil
          :tasks nil)))))


(defn get-task [data]
  (map->Task data))

(defn get-scheduler [tasks]
  (map->Scheduler {:tasks tasks}))
