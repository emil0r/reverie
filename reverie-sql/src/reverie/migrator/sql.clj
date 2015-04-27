(ns reverie.migrator.sql
  (:require [com.stuartsierra.component :as component]
            [clojure.string :as str]
            [joplin.core :as joplin]
            joplin.jdbc.database
            [reverie.migrator :refer [IMigrator]]
            [reverie.system :as sys]
            [taoensso.timbre :as log]))


(defn- get-migrator-map [{:keys [subprotocol subname user password] :as x}
                         table path]
  {:db {:type :sql
        :migration-table table
        :url (str "jdbc:" subprotocol ":"
                  subname
                  "?user=" user
                  "&password=" password)}
   :migrator path})

(defn- get-migrators []
  (let [paths (map (fn [[kw {:keys [table path]}]]
                     (let [table (or table
                                     (str
                                      "migrations"
                                      (str/replace (str kw)
                                                   #":|/|\."
                                                   "_")))]
                       [table path]))
                   (filter (fn [[_ {:keys [automatic?]}]]
                             automatic?)
                           (sys/migrations)))]
    paths))


(defrecord Migrator [database]
  IMigrator
  (migrate [this]
    (when-let [default-spec (get-in database [:db-specs :default])]
      (if (get-in default-spec [:datasource])
        (let [migrators (concat
                         [["migrations" (str "resources/migrations/"
                                             (:subprotocol default-spec))]]
                         (get-migrators))
              mmaps (map (fn [[table path]]
                           (get-migrator-map default-spec table path))
                         migrators)]
          (log/info "Starting migrations")
          (doseq [mmap mmaps]
            (log/info "Migration:" (get-in mmap [:migrator]))
            (joplin/migrate-db mmap)))))))

(defn get-migrator [database]
  (Migrator. database))
