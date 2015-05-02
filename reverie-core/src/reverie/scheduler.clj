(ns reverie.scheduler
  (:require [com.stuartsierra.component :as component]
            [cronj.core :as cron]
            [taoensso.timbre :as log]))


(defrecord Task [id desc handler pre-hook post-hook schedule opts])

(defprotocol IScheduler
  (start! [scheduler])
  (stop! [scheduler])
  (add-task! [scheduler task])
  (add-tasks! [schedular tasks]))

;; cj stands for cronjobs
(defrecord Scheduler [tasks cj]
  component/Lifecycle
  (start [this]
    (log/info "Starting scheduler")
    (if cj
      this
      (assoc this
        :cj (atom nil))))
  (stop [this]
    (log/info "Stopping scheduler")
    (if-not cj
      this
      (do
        (stop! this)
        (assoc this
          :cj nil
          :tasks nil))))
  IScheduler
  (start! [this]
    (reset! cj (cron/cronj :entries @tasks))
    (log/info "Starting cron jobs for scheduler")
    (cron/start! @cj))
  (stop! [this]
    (when @cj
      (log/info "Stopping cron jobs for scheduler")
      (cron/stop! @cj)))
  (add-task! [this task]
    (reset! tasks (into @tasks [task])))
  (add-tasks! [this tasks']
    (reset! tasks (into @tasks tasks'))))


(defn get-task [data]
  (map->Task data))

(defn get-scheduler
  ([] (map->Scheduler {:tasks (atom [])}))
  ([tasks]
     (map->Scheduler {:tasks (atom tasks)})))
