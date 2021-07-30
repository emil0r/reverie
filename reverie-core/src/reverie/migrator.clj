(ns reverie.migrator
  (:require [reverie.MigrationException]
            [reverie.system :as sys])
  (:import [reverie MigrationException]))

(defprotocol IMigrator
  (migrate [migrator] [migrator mmap])
  (rollback [migrator mmap])
  (migration-maps [migrator])
  (list-migrations [migrator mmap])
  (list-completed-ids [migrator mmap])
  (list-completed-migrations [migrator mmap])
  (list-pending-migrations [migrator mmap]))

(defn- check-type! [type]
  (let [types [:pre :module :raw-page :app :object :unknown :post]]
    (when-not (some #(= % type) types)
      (throw (MigrationException. (format "Type for exception was not valid. Only %s allowed" types))))))

(defn add-migration!
  ([name migration] (add-migration! name (or (:type migration) :unknown) migration))
  ([name type migration]
   (check-type! type)
   (swap! sys/storage assoc-in [:migrations type name] migration)))

(defn remove-migration!
  ([name] (remove-migration! name :unknown))
  ([name type]
   (check-type! type)
   (swap! sys/storage update-in [:migrations] dissoc name)))
