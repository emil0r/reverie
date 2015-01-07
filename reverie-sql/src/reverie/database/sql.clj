(ns reverie.database.sql
  (:require [clojure.java.jdbc :as jdbc]
            [com.stuartsierra.component :as component]
            [joplin.core :as joplin]
            joplin.jdbc.database
            [honeysql.core :as sql]
            [reverie.database :as db]
            [slingshot.slingshot :refer [try+]]
            [taoensso.timbre :as log])
  (:import [reverie.database DatabaseProtocol]
           [com.jolbox.bonecp BoneCPDataSource]))


(defn- bonecp-datasource
  "BoneCP based connection pool"
  [db-spec datasource]
  (let [{:keys [subprotocol subname user password]} db-spec
        {:keys [connection-timeout
                default-autocommit
                maxconns-per-partition
                minconns-per-partition
                partition-count]
         :or {connection-timeout 2000
              default-autocommit false
              maxconns-per-partition 10
              minconns-per-partition 5
              partition-count 1}} datasource]
    (assoc db-spec
      :datasource
      (doto (BoneCPDataSource.)
        (.setJdbcUrl (str "jdbc:" subprotocol ":" subname))
        (.setUsername user)
        (.setPassword password)
        (.setConnectionTestStatement "select 42;")
        (.setConnectionTimeoutInMs connection-timeout)
        (.setDefaultAutoCommit default-autocommit)
        (.setMaxConnectionsPerPartition maxconns-per-partition)
        (.setMinConnectionsPerPartition minconns-per-partition)
        (.setPartitionCount partition-count)))))


(defrecord DatabaseSQL [db-specs ds-specs]
  component/Lifecycle
  (start [this]
    (if (get-in db-specs [:default :datasource])
      this
      (do
        (let [{:keys [subprotocol
                      subname user password]} (:default db-specs)
              jmap {:db {:type :sql
                         :url (str "jdbc:" subprotocol ":"
                                   subname
                                   "?user=" user
                                   "&password=" password)}
                    :migrator (str "resources/migrations/" subprotocol)}]
          (joplin/migrate-db jmap))

        (log/info "Starting database")
        (let [db-specs (into
                        {}
                        (map
                         (fn [key]
                           (let [db-spec (get db-specs key)
                                 ds-spec (get ds-specs key)]
                             [key (bonecp-datasource db-spec ds-spec)]))
                         (keys db-specs)))]
          (assoc this
            :db-specs db-specs)))))
  (stop [this]
    (if-not (get-in db-specs [:default :datasource])
      this
      (do
        (log/info "Stopping database")
        (doseq [[_ db-spec] db-specs]
          (.close (:datasource db-spec)))
        (log/info "Closed database connection")
        (assoc this
          :db-specs (into {} (map (fn [[key db-spec]]
                                    [key (dissoc db-spec :datasource)]) db-specs))))))

  DatabaseProtocol
  (query [this query]
    (if (string? query)
      (jdbc/query (:default db-specs) [query])
      (jdbc/query (:default db-specs) (sql/format query))))
  (query [this key? query]
    (let [[key query args] (if (get db-specs key?)
                             [key? query nil]
                             [:default key? query])]
      (if (nil? args)
        (jdbc/query (get db-specs key) (sql/format query))
        (jdbc/query (get db-specs key) (sql/format query args)))))
  (query [this key query args]
    (jdbc/query (get db-specs key) (sql/format query args))))

(defn database
  ([db-specs]
     (map->DatabaseSQL {:db-specs db-specs :ds-specs {}}))
  ([db-specs ds-specs]
     (map->DatabaseSQL {:db-specs db-specs :ds-specs ds-specs})))
