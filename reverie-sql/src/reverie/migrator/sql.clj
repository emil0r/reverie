(ns reverie.migrator.sql
  (:require [com.stuartsierra.component :as component]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [migratus.core :as migratus]
            [reverie.migrator :refer [IMigrator]]
            [reverie.system :as sys]
            [reverie.util :refer [slugify]]
            [taoensso.timbre :as log]))


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
                           (let [table (or table
                                           (str
                                            "migrations_"
                                            (clojure.core/name type)
                                            "_"
                                            (str/replace (slugify name) #"-" "_")))]
                             [name type table path]))))]
    paths))

(defrecord Migrator [database]
  IMigrator
  (migrate [this]
    (when-let [ds (get-in database [:db-specs :default :datasource])]
      (let [migrations (into
                        [[:core :pre "migrations_reverie" "migrations/reverie/postgresql/"]]
                        (get-migrations))
            mmaps (map (fn [[name type table path]]
                         (get-migration-map name type ds table path))
                       migrations)]
        (log/info "Starting migrations")
        (if (every? true? (map :migration-dir-valid? mmaps))
          (doseq [mmap mmaps]
            (log/info "Migration:" (:name mmap) " " (:type mmap))
            (with-out-str (migratus/migrate mmap)))
          (do
            (let [error-data {:migrations (->> mmaps
                                               (remove :migration-dir-valid?)
                                               (map #(select-keys % [:migration-dir
                                                                     :type
                                                                     :name])))}]
              (doseq [error (:migrations error-data)]
                (log/error "Migration path does not exist" error))
              (throw (ex-info "Migration path(s) does not exist" error-data)))))))))

(defn get-migrator [database]
  (Migrator. database))
