(ns reverie.database.sql
  (:require [clojure.string :as str]
            [clojure.java.jdbc :as jdbc]
            [com.stuartsierra.component :as component]
            [joplin.core :as joplin]
            joplin.jdbc.database
            [honeysql.core :as sql]
            [reverie.auth :as auth]
            [reverie.database :as db]
            [reverie.movement :as movement]
            [reverie.object :as object]
            [reverie.page :as page]
            [reverie.publish :as publish]
            [reverie.route :as route]
            [reverie.system :as sys]
            [reverie.util :refer [slugify kw->str str->kw]]
            [slingshot.slingshot :refer [try+]]
            [taoensso.timbre :as log]
            [yesql.core :refer [defqueries]])
  (:import [reverie.database IDatabase]
           [reverie DatabaseException]
           [reverie.publish IPublish]
           [com.jolbox.bonecp BoneCPDataSource]))

(defqueries "reverie/database/sql/queries.sql")


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
              (.setPartitionCount partition-count))]
    (assoc db-spec :datasource ds)))

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

(defn- get-migrator-map [{:keys [subprotocol subname user password]} table path]
  {:db {:type :sql
        :migration-table table
        :url (str "jdbc:" subprotocol ":"
                  subname
                  "?user=" user
                  "&password=" password)}
   :migrator path})

(defn- get-migrators [system]
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
                           (sys/migrations system)))]
    paths))

(defn- recalculate-routes [db page-ids]
  (let [page-ids (if (sequential? page-ids)
                   page-ids
                   [page-ids])]
    (when-not (empty? page-ids)
      (doseq [page-id page-ids]
        (db/query! db
                   {:update :reverie_page
                    :set {:route :r.route}
                    :from [(sql/raw (str "(SELECT route FROM get_route("
                                         page-id
                                         ")) AS r"))]
                    :where [:= :id page-id]}))
      (let [page-ids (map :id
                          (db/query db
                                    {:select [:p.id]
                                     :from [[:reverie_page :p]]
                                     :join [[:reverie_page :o]
                                            [:= :o.serial :p.parent]]
                                     :where [:in :o.id page-ids]}))]
        (recur db page-ids)))))

(defn- shift-versions! [db serial]
  (let [;; pages-published
        pps (db/query db {:select [:id :version]
                          :from [:reverie_page]
                          :where [:and
                                  [:> :version 0]
                                  [:= :serial serial]]
                          :order-by [[:version :desc]]})]
    (doseq [pp pps]
      (db/query! db {:update :reverie_page
                     :set {:version (inc (:version pp))}
                     :where [:= :id (:id pp)]}))))

(def ^:dynamic *connection* nil)


(defn- get-connection [db-specs key]
  (if-not (nil? *connection*)
    (assoc (get db-specs key) :connection *connection*)
    (get db-specs key)))

(defrecord DatabaseSQL [system db-specs ds-specs]
  component/Lifecycle
  (start [this]
    (if (get-in db-specs [:default :datasource])
      this
      (do
        (let [default-spec (:default db-specs)
              migrators (concat
                         [["migrations" (str "resources/migrations/"
                                             (:subprotocol default-spec))]]
                         (get-migrators system))
              mmaps (map (fn [[table path]]
                           (get-migrator-map default-spec table path))
                         migrators)]
          (doseq [mmap mmaps]
            (joplin/migrate-db mmap)))
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
          (.close (:datasource db-spec))
          (log/info "Closed datasource for" key))
        (assoc this
          :db-specs (into
                     {} (map (fn [[key db-spec]]
                               [key (dissoc db-spec :datasource)])
                             db-specs))))))

  IDatabase
  (query [db query]
    (cond
     (string? query) (jdbc/query (get-connection db-specs :default) [query])
     (fn? query) (query {} {:connection (get-connection db-specs :default)})
     :else (jdbc/query (get-connection db-specs :default) (sql/format query))))
  (query [db key? query]
    (let [[key query args] (if (get db-specs key?)
                             [key? query nil]
                             [:default key? query])]
      (cond
       (and (nil? args) (fn? query))
       (query {} {:connection (get-connection db-specs key)})

       (fn? query)
       (query args {:connection (get-connection db-specs key)})

       (nil? args)
       (jdbc/query (get-connection db-specs key) (sql/format query))

       :else
       (jdbc/query (get-connection db-specs key) (sql/format query args)))))
  (query [db key query args]
    (if (fn? query)
      (query args {:connection (get-connection db-specs key)})
      (jdbc/query (get-connection db-specs key) (sql/format query args))))
  (query! [db query]
    (cond
     (string? query)
     (jdbc/execute! (get-connection db-specs :default) [query])

     (fn? query)
     (query {} (get-connection db-specs :default))

     :else
     (jdbc/execute! (get-connection db-specs :default) (sql/format query))))
  (query! [db key? query]
    (let [[key query args] (if (get db-specs key?)
                             [key? query nil]
                             [:default key? query])]
      (cond
       (and (fn? query) (nil? args))
       (query {} {:connection (get-connection db-specs key)})

       (fn? query)
       (query args {:connection (get-connection db-specs key)})

       (nil? args)
       (jdbc/execute! (get-connection db-specs key) (sql/format query))

       :else
       (jdbc/execute! (get-connection db-specs key) (sql/format query args)))))
  (query! [db key query args]
    (if (fn? query)
      (query args {:connection (get-connection db-specs key)})
      (jdbc/execute! (get-connection db-specs key) (sql/format query args))))

  (query<! [db query]
    (cond
     (string? query)
     (throw (DatabaseException. "String is not allowed for query<!"))

     (fn? query)
     (query {} (get-connection db-specs :default))

     :else
     (let [table (:insert-into query)
           values (:values query)]
       (apply jdbc/insert! (get-connection db-specs :default) table values))))
  (query<! [db key? query]
    (let [[key query args] (if (get db-specs key?)
                             [key? query nil]
                             [:default key? query])]
      (cond
       (and (fn? query) (nil? args))
       (query {} {:connection (get-connection db-specs key)})

       (fn? query)
       (query args {:connection (get-connection db-specs key)})

       (nil? args)
       (let [table (:insert-into query)
             values (:values query)]
         (apply jdbc/insert! (get-connection db-specs key) table values))

       :else
       (let [table (:insert-into query)
             values (:values query)]
         (apply jdbc/insert! (get-connection db-specs :default) table values)))))
  (query<! [db key query args]
    (if (fn? query)
      (query args {:connection (get-connection db-specs key)})
      (let [table (:insert-into query)
             values (:values query)]
        (apply jdbc/insert! (get-connection db-specs key) table values))))

  (databases [db]
    (keys db-specs))

  (add-page! [db data]
    (assert (contains? data :parent) ":parent key is missing")
    (assert (contains? data :template) ":template key is missing")
    (assert (contains? data :name) ":name key is missing")
    (assert (contains? data :title) ":title key is missing")
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
                 0)]
      (let [data (assoc data
                   :serial serial
                   :template (-> data :template kw->str)
                   :app (-> data :app kw->str)
                   :type (-> data :type kw->str)
                   :slug (or (:slug data)
                             (slugify (:name data)))
                   :order (inc order)
                   :version 0)
            {:keys [id] :as page-data} (db/query! db add-page<! data)]
        (recalculate-routes db id)
        (auth/add-authorization! (db/get-page db id)
                                 db
                                 :all
                                 :view)
        (auth/add-authorization! (db/get-page db id)
                                 db
                                 :edit
                                 :staff)
        (auth/add-authorization! (db/get-page db id)
                                 db
                                 :add
                                 :staff)
        page-data)))

  (update-page! [db id data]
    (let [data (select-keys data [:template :name :title
                                  :route :type :app :slug])]
      (assert (not (empty? data)) "update-page! does not take an empty data set")
      (db/query! db {:update :reverie_page
                     :set data})
      (recalculate-routes db id)
      true))

  (move-page! [db id origo-id movement]
    (let [movement (keyword movement)]
      (assert id "id has to be non-nil")
      (assert origo-id "origo-id has to be non-nil")
      (assert (some #(= % movement) [:after :before]) "movement has to be :after or :before")
      (let [parent
            (-> (db/query db {:select [:parent]
                              :from [:reverie_page]
                              :where [:= :id origo-id]})
                first :parent)
            parent-id
            (-> (db/query db {:select [:parent]
                              :from [:reverie_page]
                              :where [:= :id id]})
                first :parent)
            objs
            (->> (db/query db {:select [:p.order :p.id]
                               :from [[:reverie_page :p]]
                               :join [[:reverie_page :o]
                                      [:= :p.parent :o.parent]]
                               :where [:and
                                       [:= :p.version 0]
                                       [:= :o.id origo-id]]
                               :order-by [(sql/raw "\"order\"")]})
                 (map (fn [{:keys [order id]}]
                        [order id])))
            objs
            (cond
             (and (= parent parent-id)
                  (= movement :after))
             (movement/move objs id :down)

             (and (= parent parent-id)
                  (= movement :before))
             (movement/move objs id :up)

             (= movement :after)
             (movement/after objs origo-id id)

             :else
             (movement/before objs origo-id id))]
        (doseq [[order id] objs]
          (db/query! db {:update :reverie_page
                         :set {(sql/raw "\"order\"") order
                               :parent parent}
                         :where [:= :id id]}))
        (recalculate-routes db id))))

  (add-object! [db data]
    (assert (contains? data :page_id) ":page_id key is missing")
    (assert (contains? data :area) ":area key is missing")
    (assert (contains? data :route) ":route key is missing")
    (assert (contains? data :name) ":name key is missing")
    (assert (contains? data :properties) ":properties key is missing")

    (let [order (or (-> (db/query db {:select [(sql/raw "max(\"order\")")]
                                      :from [:reverie_object]
                                      :where [:= :page_id (:page_id data)]})
                        first
                        :max)
                    1)
          obj (db/query! db add-object<! (-> data
                                             (assoc :order order)
                                             (dissoc :properties)))
          obj-meta (sys/object system (-> data :name keyword))
          field-ks (->> obj-meta :options :properties keys)
          fk (or (->> obj-meta :options :foreign-key)
                 :object_id)
          table (get-in obj-meta [:options :table])
          obj-id (:id obj)
          properties (merge (select-keys (:properties data) field-ks)
                            {fk obj-id})]
      (db/query! db {:insert-into (sql/raw table)
                     :values [properties]})
      obj))

  (update-object! [db id data]
    (let [obj-name (-> (db/query db {:select [:name]
                                     :from [:reverie_object]
                                     :where [:= :id id]})
                       first :name keyword)
          obj-meta (sys/object system obj-name)
          fk (or (->> obj-meta :options :foreign-key)
                 :object_id)
          field-ks (->> obj-meta :options :properties keys)
          table (or (get-in obj-meta [:options :table])
                    obj-name)
          properties (merge (select-keys data field-ks))]
      (assert (not (empty? properties))
              "update-object! does not take an empty data set")
      (db/query! db {:update (sql/raw table)
                     :set properties
                     :where [:= fk id]})))

  (move-object! [db id direction]
    (assert (some #(= % (keyword direction)) [:up :down :bottom :top])
            "direction has to be :up, :down, :bottom or :top")
    (let [objs (->
                (map
                 (fn [{:keys [order id]}]
                   [order id])
                 (db/query db {:select [:o.order :o.id]
                               :from [[:reverie_object :f]]
                               :join [[:reverie_object :o]
                                      [:= :f.page_id :o.page_id]]
                               :where [:and
                                       [:= :f.id id]
                                       [:= :f.area :o.area]]}))
                (movement/move id direction :origo))]
      (doseq [[order id] objs]
        (if-not (nil? id)
          (db/query! db {:update :reverie_object
                         :set {(sql/raw "\"order\"") order}
                         :where [:= :id id]})))))

  (move-object! [db id page-id area]
    (let [area (kw->str area)
          order (-> (db/query db {:select [:%max.o.order]
                                  :from [[:reverie_object :o]]
                                  :where [:and
                                          [:= :o.page_id page-id]
                                          [:= :o.area area]]})
                    first :max inc)]
      (db/query! db {:update :reverie_object
                     :set {(sql/raw "\"order\"") order
                           :area area
                           :page_id page-id}
                     :where [:= :id id]})))

  (move-object-to-object! [db id other-id direction]
    (assert (some #(= % (keyword direction)) [:after :before])
            "direction has to be :after or :before")
    (let [direction (keyword direction)
          {:keys [area page_id]}
          (-> (db/query db {:select [:area :page_id]
                            :from [:reverie_object]
                            :where [:= :id other-id]})
              first)
          objs (->> (db/query db {:select [:o.order :o.id]
                                  :from [[:reverie_object :o]]
                                  :join [[:reverie_object :j]
                                         [:= :j.page_id :o.page_id]]
                                  :where [:= :j.id other-id]
                                  :order-by [:o.order]})
                    (map (fn [{:keys [order id]}] [order id])))
          objs (if (= direction :after)
                 (movement/after objs other-id id :origo)
                 (movement/before objs other-id id :origo))]
      (doseq [[order id] objs]
        (db/query! db {:update :reverie_object
                       :set {(sql/raw "\"order\"") order
                             :page_id page_id}
                       :where [:= :id id]}))))

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
                        :where [:and
                                [:= :page_id (page/id page)]
                                [:not= :version -1]]
                        :order-by [(sql/raw "\"order\"")]})

          objs-properties-to-fetch
          (reduce (fn [out {:keys [name] :as obj-meta}]
                    (let [obj-data (get-in @sys/storage
                                           [:objects (keyword name)])
                          table (:table obj-data)
                          foreign-key (or (get-in obj-data [:options :foreign-key])
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
                         {obj-id (dissoc data :id foreign-key)}))
                     (db/query db {:select [:*]
                                   :from [table]
                                   :where [:in foreign-key object-ids]}))))
                 objs-properties-to-fetch)))]
      (map (fn [{:keys [id name] :as obj-meta}]
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

  IPublish

  (publish-page! [db page-id]
    (publish/publish-page! db page-id false))

  (publish-page! [db page-id recur?]
    ;; TODO: wrap in a transaction
    ;; TODO: rewrite so it doesn't use so many db calls :(
    (let [;; page-unpublished
          pu (db/get-page db page-id)
          pages (if recur?
                  (map (fn [{:keys [serial]}]
                         (db/get-page db serial false))
                       (db/query db (str "SELECT * FROM get_serials_recursively(" (page/serial pu) ");")))
                  [pu])]
      (doseq [pu pages]
        (shift-versions! db (page/serial pu))
        (let [{:keys [id] :as copied} (db/query! db copy-page<! {:id (page/id pu)})]
          (doseq [obj (page/objects pu)]
            (let [;; copy meta object
                  new-meta-obj (db/query! db copy-object-meta<!
                                          {:pageid id
                                           :id (object/id obj)})
                  ;; get meta data for meta object
                  obj-meta (sys/object system (-> new-meta-obj :name keyword))
                  ;; foreign key
                  fk (or (->> obj-meta :options :foreign-key)
                         :object_id)]
              (db/query! db {:insert-into (:table obj-meta)
                             :values [(assoc (object/properties obj)
                                        fk (:id new-meta-obj))]}))))
        (recalculate-routes db (page/id pu))
        (db/query! db update-published-pages-order! {:parent (page/parent pu)}))))

  (unpublish-page! [db page-id]
    (let [;; page-unpublished
          pu (db/get-page db page-id)
          pages (map (fn [{:keys [serial]}]
                       (db/get-page db serial false))
                     (db/query db (str "SELECT * FROM get_serials_recursively(" (page/serial pu) ");")))]
      (doseq [pu pages]
        (shift-versions! db (page/serial pu)))))

  (trash-page! [db page-id]
    (let [;; page-unpublished
          pu (db/get-page db page-id)
          pages (map (fn [{:keys [serial]}]
                       (db/get-page db serial false))
                     (db/query db (str "SELECT * FROM get_serials_recursively(" (page/serial pu) ");")))]
      (doseq [pu pages]
        (shift-versions! db (page/serial pu))
        (db/query! db {:update :reverie_page
                       :set {:version -1}
                       :where [:= :id (page/id pu)]}))))

  (trash-object! [db obj-id]
    (db/query! db {:update :reverie_object
                   :set {:version -1}
                   :where [:= :id obj-id]})))

(defn database
  ([db-specs]
     (map->DatabaseSQL {:db-specs db-specs :ds-specs {}}))
  ([db-specs ds-specs]
     (map->DatabaseSQL {:db-specs db-specs :ds-specs ds-specs})))
