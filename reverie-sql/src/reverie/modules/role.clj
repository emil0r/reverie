(ns reverie.modules.role
  (:require [clojure.set :as set]
            [com.stuartsierra.component :as component]
            [reverie.database :as db]
            [reverie.system :as sys]
            [reverie.util :as util]
            [taoensso.timbre :as log]))


(defrecord RoleManager [database system]
  component/Lifecycle
  (start [this]
    (log/info "Starting role manager")
    (let [roles
          (apply set/union
                 (remove nil?
                         (map (fn [data]
                                (get-in (-> data vals first) [:options :roles]))
                              (flatten [(sys/objects system)
                                        (sys/raw-pages system)
                                        (sys/modules system)
                                        (sys/objects system)]))))
          roles-in-db (into #{}
                            (map #(-> % :name keyword)
                                 (db/query database {:select [:name]
                                                     :from [:auth_role]})))
          roles-diff (set/difference roles roles-in-db)]
      (when-not (empty? roles-diff)
        (db/query! database {:insert-into :auth_role
                             :values (map (fn [role]
                                            {:name (util/kw->str role)}) roles-diff)}))))
  (stop [this]
    (log/info "Stopping role manager")
    this))
