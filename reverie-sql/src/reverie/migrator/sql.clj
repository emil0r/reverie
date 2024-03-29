(ns reverie.migrator.sql
  (:require [com.stuartsierra.component :as component]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [migratus.core :as migratus]
            [migratus.migrations :as migratus.mig]
            [migratus.protocols :as migratus.proto]
            [reverie.migrator :refer [IMigrator]]
            [reverie.system :as sys]
            [reverie.util :refer [slugify]]
            [taoensso.timbre :as log]))

(defn get-migration-table [{:keys [table type name]}]
  (or table
      (str
       "migrations_"
       (clojure.core/name type)
       "_"
       (str/replace (slugify name) #"-" "_"))))

(defn get-migration-map [type name datasource table path]
  (let [mmap {:store :database
              :migration-table-name table
              :migration-dir path
              :db datasource
              :type type
              :name name}]
    (assoc mmap :migration-dir-valid? (some? (io/resource path)))))

(defn- get-migrations []
  (let [paths (->> (sys/migrations)
                   (filter (fn [[_ {:keys [automatic?]}]]
                             automatic?))
                   (reduce (fn [out [kw {:keys [type] :or {type :unknown} :as migration}]]
                             (assoc out type (conj (get out type) [kw migration])))
                           (array-map :pre []
                                      :module []
                                      :raw-page []
                                      :app []
                                      :object []
                                      :unknown []
                                      :post []))
                   (vals)
                   (flatten)
                   (partition 2)
                   (mapv (fn [[name {:keys [table path type]}]]
                           (let [table (get-migration-table {:table table :type type :name name})]
                             [name type table path]))))]
    paths))

(defrecord Migrator [database]
  IMigrator
  (migrate [this]
    (let [mmaps (reverie.migrator/migration-maps this)]
      (log/info "Starting migrations")
      (if (every? true? (map :migration-dir-valid? mmaps))
        (doseq [mmap mmaps]
          (reverie.migrator/migrate this mmap))
        (do
          (let [error-data {:migrations (->> mmaps
                                             (remove :migration-dir-valid?)
                                             (map #(select-keys % [:migration-dir
                                                                   :type
                                                                   :name])))}]
            (doseq [error (:migrations error-data)]
              (log/error "Migration path does not exist" error))
            (throw (ex-info "Migration path(s) does not exist" error-data)))))))
  (migrate [this mmap]
    (log/info (format "Migrating %s %s" (:name mmap)  (:type mmap)))
    (with-out-str (migratus/migrate mmap)))
  (migration-maps [this]
    (when-let [ds (get-in database [:db-specs :default :datasource])]
      (let [migrations (into
                        [[:core :pre "migrations_reverie" "migrations/reverie/postgresql/"]]
                        (get-migrations))
            mmaps (map (fn [[name type table path]]
                         (get-migration-map name type ds table path))
                       migrations)]
        mmaps)))
  (rollback [this mmap]
    (log/info (format "Rolling back %s %s" (:name mmap)  (:type mmap)))
    (with-out-str (migratus/rollback mmap)))
  (list-migrations [this mmap]
    (migratus.mig/list-migrations mmap))
  (list-completed-ids [this mmap]
    (let [store (migratus.proto/make-store mmap)]
      (migratus.proto/connect store)
      (let [ids (migratus.proto/completed-ids store)]
        (migratus.proto/disconnect store)
        ids)))
  (list-completed-migrations [this mmap]
    (migratus/completed-list mmap))
  (list-pending-migrations [this mmap]
    (migratus/pending-list mmap)))

(defn get-migrator [database]
  (Migrator. database))
