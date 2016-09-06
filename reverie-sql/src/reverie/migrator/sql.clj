(ns reverie.migrator.sql
  (:require [com.stuartsierra.component :as component]
            [clojure.string :as str]
            [joplin.core :as joplin]
            joplin.jdbc.database
            [reverie.migrator :refer [IMigrator]]
            [reverie.system :as sys]
            [reverie.util :refer [slugify]]
            [taoensso.timbre :as log]))


(defn- get-migrator-map [{:keys [subprotocol subname user password] :as x}
                         table path]
  {:db {:type :sql
        :migrations-table table
        :url (str "jdbc:" subprotocol ":"
                  subname
                  "?user=" user
                  "&password=" password)}
   :migrator path})

(defn- get-migrators []
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
                   (mapv (fn [[kw {:keys [table path]}]]
                           (let [table (or table
                                           (str
                                            "migrations_"
                                            (str/replace (slugify kw)  #"-" "_")))]
                             [table path]))))]
    paths))

(defrecord Migrator [database]
  IMigrator
  (migrate [this]
    (when-let [default-spec (get-in database [:db-specs :default])]
      (if (get-in default-spec [:datasource])
        (let [migrators (concat
                         [["migrations" (str "resources/migrations/reverie/"
                                             (:subprotocol default-spec))]]
                         (get-migrators))
              mmaps (map (fn [[table path]]
                           (get-migrator-map default-spec table path))
                         migrators)]
          (log/info "Starting migrations")
          (doseq [mmap mmaps]
            (log/info "Migration:" (get-in mmap [:migrator]))
            (with-out-str (joplin/migrate-db mmap))))))))

(defn get-migrator [database]
  (Migrator. database))
