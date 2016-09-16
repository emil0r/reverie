(ns reverie.admin
  (:require [com.stuartsierra.component :as component]
            [reverie.admin.api.editors :refer [edits editors]]
            [reverie.admin.storage :as admin.storage]
            [reverie.database :as db]
            [reverie.internal :as internal]
            [reverie.internal.memory :as internal.memory]
            [reverie.time :as time]
            [taoensso.timbre :as log]))

(defn- get-edits []
  (->> @edits
       (map (fn [[k {:keys [time] :as v}]]
              [k (assoc v :time (time/coerce time :java.sql.Timestamp))]))
       (into {})))

(defn- reverse-edits [edits]
  (->> edits
       (map (fn [[k {:keys [time] :as v}]]
              [k (assoc v :time (time/coerce time :joda))]))
       (into {})))

(defn- get-editors []
  (->> @editors
       (map (fn [[k v]]
              [k (time/coerce v :java.sql.Timestamp)]))
       (into {})))

(defn- reverse-editors [editors]
  (->> editors
       (map (fn [[k v]]
              [k (time/coerce v :joda)]))
       (into {})))

(defrecord AdminInitializer [database store]
  component/Lifecycle
  (start [this]
    (log/info "Starting AdminInitializer")
    (let [store (or store (internal.memory/mem-store))]
      (try
        (let [saved-edits (admin.storage/get database :admin.storage/edits)
              saved-editors (admin.storage/get database :admin.storage/editors)]
          (reset! edits (reverse-edits saved-edits))
          (reset! editors (reverse-editors saved-editors)))
        (reset! internal/storage store)
        (db/cache-pages database)
        (catch Exception e
          (log/error {:what ::AdminInitializer
                      :message (.getMessage e)
                      :exception e})))
      (assoc this :store store)))
  (stop [this]
    (log/info "Stopping AdminInitializer")
    (let [edits (get-edits)
          editors (get-editors)]
      (admin.storage/assoc! database :admin.storage/edits edits)
      (admin.storage/assoc! database :admin.storage/editors editors))
    this))

(defn get-admin-initializer
  ([]
   (map->AdminInitializer {}))
  ([settings]
   (map->AdminInitializer settings)))
