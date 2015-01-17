(ns reverie.database.sql
  (:require [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]
            [com.stuartsierra.component :as component]
            [joplin.core :as joplin]
            joplin.jdbc.database
            [honeysql.core :as sql]
            [reverie.database :as db]
            [reverie.object :as object]
            [reverie.page :as page]
            [reverie.publish :as publish]
            [reverie.route :as route]
            [reverie.system :as sys]
            [slingshot.slingshot :refer [try+]]
            [taoensso.timbre :as log])
  (:import [reverie.database DatabaseProtocol]
           [reverie.publish PublishingProtocol]
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
              partition-count 1}} datasource
         ds (doto (BoneCPDataSource.)
              (.setJdbcUrl (str "jdbc:" subprotocol ":" subname))
              (.setUsername user)
              (.setPassword password)
              (.setConnectionTestStatement "select 42;")
              (.setConnectionTimeoutInMs connection-timeout)
              (.setDefaultAutoCommit default-autocommit)
              (.setMaxConnectionsPerPartition maxconns-per-partition)
              (.setMinConnectionsPerPartition minconns-per-partition)
              (.setPartitionCount partition-count))
         db-spec (assoc db-spec :datasource ds)
         connection (jdbc/get-connection db-spec)]
    (assoc db-spec :connection connection)))

(defn- massage-page-data [data]
  (assoc data
    :type (keyword (:type data))
    :template (keyword (:template data))
    :app (if (str/blank? (:app data))
           ""
           (keyword (:app data)))))


(defn- get-page [database data]
  (if data
    (let [{:keys [template app] :as data} (massage-page-data data)]
      (case (:type data)
        :page (let [p (page/page
                       (assoc data
                         :template (get-in @sys/storage
                                           [:templates template])
                         :database database))]
                (assoc p :objects (db/get-objects database p)))
        :app (let [page-data (get-in @sys/storage
                                     [:apps app])
                   p (page/app-page
                      (assoc data
                        :template (get-in @sys/storage
                                          [:templates template])
                        :options (:options page-data)
                        :app-routes (:app-routes page-data)
                        :database database))]
               (assoc p :objects (db/get-objects database p)))))))

(defn- get-migration-map [{:keys [subprotocol subname user password]} path]
  {:db {:type :sql
        :url (str "jdbc:" subprotocol ":"
                  subname
                  "?user=" user
                  "&password=" password)}
   :migrator path})

(defn- get-migration-paths [system]
  (let [paths (sort
               (map (fn [[_ {:keys [path]}]]
                      path)
                    (filter (fn [[_ {:keys [automatic?]}]]
                              automatic?)
                            (sys/migrations system))))]
    paths))

(defn- kw->str [x]
  (if (keyword? x)
    (str/replace (str x) #":" "")
    x))

(defrecord DatabaseSQL [system db-specs ds-specs]
  component/Lifecycle
  (start [this]
    (if (get-in db-specs [:default :datasource])
      this
      (do
        (let [default-spec (:default db-specs)
              migration-paths (get-migration-paths system)
              paths (if (empty? migration-paths)
                      [(str "resources/migrations/"
                            (:subprotocol default-spec))]
                      (apply conj
                             [(str "resources/migrations/"
                            (:subprotocol default-spec))]
                             migration-paths))
              mmap (get-migration-map default-spec paths)]
          (joplin/migrate-db mmap))

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
        (doseq [[key db-spec] db-specs]
          (.close (:connection db-spec))
          (log/info "Closed connection for" key)
          (.close (:datasource db-spec))
          (log/info "Closed datasource for" key))
        (assoc this
          :db-specs (into
                     {} (map (fn [[key db-spec]]
                               [key (dissoc db-spec :datasource)])
                             db-specs))))))

  DatabaseProtocol
  (query [db query]
    (if (string? query)
      (jdbc/query (:default db-specs) [query])
      (jdbc/query (:default db-specs) (sql/format query))))
  (query [db key? query]
    (let [[key query args] (if (get db-specs key?)
                             [key? query nil]
                             [:default key? query])]
      (if (nil? args)
        (jdbc/query (get db-specs key) (sql/format query))
        (jdbc/query (get db-specs key) (sql/format query args)))))
  (query [db key query args]
    (jdbc/query (get db-specs key) (sql/format query args)))
  (query! [db query]
    (if (string? query)
      (jdbc/execute! (:default db-specs) [query])
      (jdbc/execute! (:default db-specs) (sql/format query))))
  (query! [db key? query]
    (let [[key query args] (if (get db-specs key?)
                             [key? query nil]
                             [:default key? query])]
      (if (nil? args)
        (jdbc/execute! (get db-specs key) (sql/format query))
        (jdbc/execute! (get db-specs key) (sql/format query args)))))
  (query! [db key query args]
    (jdbc/execute! (get db-specs key) (sql/format query args)))
  (databases [db]
    (keys db-specs))
  (add-page! [db data]
    (assert (contains? data :parent) ":parent key is missing")
    (assert (contains? data :template) ":template key is missing")
    (assert (contains? data :name) ":name key is missing")
    (assert (contains? data :title) ":title key is missing")
    (assert (contains? data :route) ":route key is missing")
    (assert (contains? data :type) ":type key is missing")
    (assert (contains? data :app) ":app key is missing")
    (let [serial (-> (db/query db {:select [:%max.serial]
                                   :from [:reverie_page]})
                     first
                     :max
                     inc)
          order (or
                 (-> (db/query db {:select [(sql/raw "max(\"order\")")]
                                   :from [:reverie_page]
                                   :where [:and
                                           [:= :parent (:parent data)]
                                           [:= :version 0]]})
                     first
                     :max)
                 1)]
      (let [data (assoc data
                   :id (sql/raw "default")
                   :serial serial
                   :template (-> data :template kw->str)
                   :app (-> data :app kw->str)
                   :type (-> data :type kw->str)
                   (sql/raw "\"order\"") (inc order)
                   :version 0)]
        (db/query! db {:insert-into :reverie_page
                       :values [data]}))))
  (add-object! [db data]
    )

  (get-pages [db]
    (map (partial get-page db)
         (db/query db {:select [:*]
                       :from [:reverie_page]
                       :where [:or
                               [:= :version 0]
                               [:= :version 1]]
                       :order-by [(sql/raw "\"order\"")]})))
  (get-pages [db published?]
    (map (partial get-page db)
         (db/query db {:select [:*]
                       :from [:reverie_page]
                       :where [:= :version (if published? 1 0)]
                       :order-by [(sql/raw "\"order\"")]})))
  (get-pages-by-route [db]
    (map (fn [page-data]
           [(:route page-data) page-data])
         (map massage-page-data
              (db/query db {:select [:*]
                            :from [:reverie_page]
                            :where [:or
                                    [:= :version 0]
                                    [:= :version 1]]
                            :order-by [(sql/raw "\"order\"")]}))))
  (get-page [db id]
    (get-page db (first (db/query db {:select [:*]
                                      :from [:reverie_page]
                                      :where [:= :id id]}))))
  (get-page [db serial published?]
    (get-page db (first (db/query db {:select [:*]
                                      :from [:reverie_page]
                                      :where [:and
                                              [:= :serial serial]
                                              [:= :version (if published? 1 0)]]}))))
  (get-children [db page]
    (let [serial (if (number? page)
                   page
                   (page/serial page))]
      (map (partial get-page db)
           (db/query db {:select [:*]
                         :from [:reverie_page]
                         :where [:and
                                 [:= :version (page/version page)]
                                 [:= :parent serial]]
                         :order-by [(sql/raw "\"order\"")]}))))
  (get-children-count [db page]
    (let [serial (if (number? page)
                   page
                   (page/serial page))
          version (page/version page)]
      (-> (db/query db {:select [:%count.*]
                        :from [:reverie_page]
                        :where [:and
                                [:= :version version]
                                [:= :parent serial]]})
          first
          :count)))

  (get-objects [db page]
    (let [objs-meta
          (db/query db {:select [:*]
                        :from [:reverie_object]
                        :where [:= :page_id (page/id page)]
                        :order-by [(sql/raw "\"order\"")]})

          objs-properties-to-fetch
          (reduce (fn [out {:keys [name] :as obj-meta}]
                    (let [obj-data (get-in @sys/storage
                                           [:objects (keyword name)])
                          table (keyword
                                 (or (get-in obj-data [:options :table])
                                     (str/replace name #"/|\." "_")))
                          foreign-key (or (get-in obj-data [:options :table :foreign-key])
                                          :object_id)
                          object-ids (get (get out name)
                                          :object-ids [])]
                      (assoc out name
                             {:table table
                              :foreign-key foreign-key
                              :object-ids (conj object-ids (:id obj-meta))})))
                  {} objs-meta)

          objs-properties
          (into
           {}
           (flatten
            (map (fn [[name {:keys [table foreign-key object-ids]}]]
                   (into
                    {}
                    (map
                     (fn [data]
                       (let [obj-id (get data foreign-key)]
                         {obj-id data}))
                     (db/query db {:select [:*]
                                   :from [table]
                                   :where [:in foreign-key object-ids]}))))
                 objs-properties-to-fetch)))]
      (map (fn [{:keys [id] :as obj-meta}]
             (let [obj-data (get-in @sys/storage
                                    [:objects (keyword (:name obj-meta))])]
               (object/object
                (assoc obj-meta
                  :properties (get objs-properties id)
                  :database db
                  :page page
                  :route (route/route [(:route obj-meta)])
                  :area (keyword (:area obj-meta))
                  :name (keyword (:name obj-meta))
                  :options (:options obj-data)
                  :methods (:methods obj-data)))))
           objs-meta)))
  PublishingProtocol)

(defn database
  ([db-specs]
     (map->DatabaseSQL {:db-specs db-specs :ds-specs {}}))
  ([db-specs ds-specs]
     (map->DatabaseSQL {:db-specs db-specs :ds-specs ds-specs})))
