(ns reverie.migrator.sql
  (:require [com.stuartsierra.component :as component]
            [clojure.string :as str]
            [migratus.core :as migratus]
            [reverie.migrator :refer [IMigrator]]
            [reverie.system :as sys]
            [reverie.util :refer [slugify]]
            [taoensso.timbre :as log]))


(defn get-migration-map [type name datasource table path]
  {:store :database
   :migration-table-name table
   :migration-dir path
   :db datasource
   :type type
   :name name})

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
                                            (str/replace (slugify name) #"-" "_")
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
        (doseq [mmap mmaps]
          (log/info "Migration:" (:name mmap) " " (:type mmap))
          (with-out-str (migratus/migrate mmap)))))))

(defn get-migrator [database]
  (Migrator. database))
