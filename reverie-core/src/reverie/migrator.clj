(ns reverie.migrator
  (:require [reverie.system :as sys]))

(defprotocol IMigrator
  (migrate [migrator]))


(defn add-migration! [name migration]
  (swap! sys/storage assoc-in [:migrations name] migration))

(defn remove-migration! [name]
  (swap! sys/storage update-in [:migrations] dissoc name))
