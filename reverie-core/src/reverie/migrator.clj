(ns reverie.migrator)

(defprotocol IMigrator
  (migrate [migrator]))
