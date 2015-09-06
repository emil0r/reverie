(ns reverie.modules.role
  (:require [clojure.set :as set]
            [com.stuartsierra.component :as component]
            [ez-database.core :as db]
            [reverie.system :as sys]
            [reverie.util :as util]
            [taoensso.timbre :as log]))

(defrecord RoleManager [database]
  component/Lifecycle
  (start [this]
    (log/info "Starting role manager")
    (let [roles
          (apply set/union
                 (remove nil?
                         (flatten
                          (map (fn [data]
                                 (map #(get-in % [:options :roles])
                                      (vals data)))
                               [(sys/objects)
                                (sys/raw-pages)
                                (sys/modules)
                                (sys/objects)]))))
          roles-in-db (into #{}
                            (map #(-> % :name keyword)
                                 (db/query database {:select [:name]
                                                     :from [:auth_role]})))
          roles-diff (set/difference roles roles-in-db)]
      (when-not (empty? roles-diff)
        (db/query! database {:insert-into :auth_role
                             :values (map (fn [role]
                                            {:name (util/kw->str role)}) roles-diff)})))
    this)
  (stop [this]
    (log/info "Stopping role manager")
    this))


(defn get-rolemanager []
  (map->RoleManager {}))
