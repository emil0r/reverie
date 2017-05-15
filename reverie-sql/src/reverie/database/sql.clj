(ns reverie.database.sql
  (:require [clj-time.coerce :as t.coerce]
            [clojure.edn :as edn]
            [clojure.java.jdbc :as jdbc]
            [clojure.set :as set]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [ez-database.core :as db]
            [hikari-cp.core :as hikari-cp]
            [honeysql.core :as sql]
            [joplin.core :as joplin]
            [joplin.jdbc.database]
            [noir.session :as session]
            [reverie.auth :as auth :refer [IUserDatabase]]
            [reverie.database :as rev.db :refer [IDatabase]]
            [reverie.internal :as internal :refer [storage]]
            [reverie.movement :as movement]
            [reverie.object :as object]
            [reverie.page :as page]
            [reverie.publish :as publish :refer [IPublish]]
            [reverie.route :as route]
            [reverie.site :as site]
            [reverie.system :as sys]
            [reverie.util :refer [slugify kw->str str->kw uuid?]]
            [schema.core :as s]
            [slingshot.slingshot :refer [try+ throw+]]
            [taoensso.timbre :as log]
            [yesql.core :refer [defqueries]])
  (:import [ez_database.core EzDatabase]
           [reverie DatabaseException]))


; http://clojure.github.io/java.jdbc/#clojure.java.jdbc/IResultSetReadColumn
(extend-protocol jdbc/IResultSetReadColumn
  org.postgresql.jdbc.PgArray
  (result-set-read-column [pgobj metadata i]
    (vec (.getArray pgobj)))
  java.sql.Timestamp
  (result-set-read-column [v _2 _3]
    (t.coerce/from-sql-time v))
  java.sql.Date
  (result-set-read-column [v _2 _3]
    (t.coerce/from-sql-date v))
  java.sql.Time
  (result-set-read-column [v _2 _3]
    (org.joda.time.DateTime. v)))

; http://clojure.github.io/java.jdbc/#clojure.java.jdbc/ISQLValue
(extend-protocol jdbc/ISQLValue
  org.joda.time.DateTime
  (sql-value [v]
    (t.coerce/to-sql-time v)))


(defqueries "queries/database.sql")

;; internal storage

(defn- get-stored-page
  ([id]
   (if-let [page (internal/read-storage @storage [:reverie/page id])]
     (assoc page
            :template (get-in @sys/storage
                              [:templates (keyword (get-in page [:raw-data :template]))]))))
  ([serial published?]
   (if-let [page (internal/read-storage @storage [:reverie/page serial published?])]
     (assoc page
            :template (get-in @sys/storage
                              [:templates (keyword (get-in page [:raw-data :template]))])))))

(defn- get-stored-pages [ids]
  (reduce (fn [out id]
            (if-let [page (internal/read-storage @storage [:reverie/page id])]
              (conj out (assoc page
                               :template (get-in @sys/storage
                                                 [:templates (keyword (get-in page [:raw-data :template]))])))
              out))
          [] ids))

(defn- store-page [page]
  (when-not (nil? page)
    ;; remove template (a function)
    (let [p (assoc page :template nil)]
      (internal/write-storage @storage [:reverie/page (page/id page)] p)
      (internal/write-storage @storage [:reverie/page (page/serial page) (if (zero? (page/version p)) false true)] p)
      (let [pages (or (internal/read-storage @storage :reverie/pages) #{})]
        (internal/write-storage @storage :reverie/pages (set/union pages #{(page/serial page)})))))
  page)

(defn- delete-stored-page
  ([id]
   (when-let [page (internal/read-storage @storage [:reverie/page id])]
     (internal/delete-storage @storage [:reverie/page id])
     (internal/delete-storage @storage [:reverie/page (page/serial page) (page/published? page)])))
  ([serial published?]
   (when-let [page (internal/read-storage @storage [:reverie/page serial published?])]
     (internal/delete-storage @storage [:reverie/page (page/id page)])
     (internal/delete-storage @storage [:reverie/page serial published?]))))

;; end of internal storage

(defn- massage-page-data [data]
  (-> data
      (assoc :published? (:published_p data)
             :raw-data data
             :type (keyword (:type data))
             :properties (->> data
                              :properties
                              (reduce (fn [out x]
                                        (let [[a b] (str/split x #":")]
                                          (assoc-in out (map keyword (str/split a #"_")) (edn/read-string b))))
                                      {}))
             :template (keyword (:template data))
             :app (if (str/blank? (:app data))
                    ""
                    (keyword (:app data))))
      (dissoc :published_p)))

(defn- get-page [database data]
  (let [page (if data
              (let [{:keys [template app] :as data} (massage-page-data data)]
                (case (:type data)
                  :page (let [p (page/page
                                 (assoc data
                                        :template (get-in @sys/storage
                                                          [:templates template])
                                        :raw-data (:raw-data data)))
                              objects (map #(assoc % :page p) (rev.db/get-objects database p))]
                          (assoc p :objects objects))
                  :app (let [page-data (get-in @sys/storage
                                               [:apps app])
                             p (page/app-page
                                (assoc data
                                       :template (get-in @sys/storage
                                                         [:templates template])
                                       :options (:options page-data)
                                       :app-routes (:app-routes page-data)
                                       :raw-data (:raw-data data)))
                             objects (map #(assoc % :page p) (rev.db/get-objects database p))]
                         (assoc p :objects objects)))))]
    (if (:dev? database)
      page
      (store-page page))))

(defn- recalculate-routes-db [db page-ids]
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
                                     :where [:and
                                             [:in :o.id page-ids]
                                             [:= :p.version 0]
                                             [:= :o.version 0]]}))]
        (recur db page-ids)))))

(defn- recalculate-routes [db page-ids]
  (recalculate-routes-db db page-ids)
  (site/reset-routes! (sys/get-site)))

(defn- shift-versions! [db serial]
  (let [;; pages-published
        pps (db/query db {:select [:id :version]
                          :from [:reverie_page]
                          :where [:and
                                  [:> :version 0]
                                  [:= :serial serial]]
                          :order-by [[:version :desc]]})]
    (db/with-transaction [db :default]
      (doseq [pp pps]
        (db/query! db {:update :reverie_page
                       :set {:version (inc (:version pp))}
                       :where [:= :id (:id pp)]})))))

;; user fns

(defn- extend-user [user db]
  (reduce (fn [user [k v]]
            (cond (fn? v) (assoc user k (delay (v {:database db :user user})))
                  :else (assoc k (delay v))))
          user auth/*extended*))

(defn- get-user-roles [roles groups]
  (->> [roles
        (map (fn [[_ roles]] roles) groups)]
       (flatten)
       (map keyword)
       (into #{})))


(defn- get-user-groups [groups]
  (->> (map (fn [[group _]] group) groups)
       (flatten)
       (map keyword)
       (into #{})))

;; datasource

(defn- get-datasource
  "HikaruCP based connection pool"
  [db-spec datasource]
  (let [{:keys [subprotocol subname user password]} db-spec
        ds (hikari-cp/make-datasource datasource)]
    (assoc db-spec :datasource ds)))


(extend-type EzDatabase
  component/Lifecycle
  (start [this]
    (let [{:keys [db-specs ds-specs]} this]
      (if (get-in db-specs [:default :datasource])
        this
        (do
          (log/info "Starting database")
          (let [db-specs (into
                          {}
                          (map
                           (fn [key]
                             (let [db-spec (get db-specs key)
                                   ds-spec (get ds-specs key)]
                               [key (get-datasource db-spec ds-spec)]))
                           (keys db-specs)))]
            (assoc this :db-specs db-specs :ds-specs ds-specs))))))
  (stop [this]
    (let [db-specs (:db-specs this)]
      (if-not (get-in db-specs [:default :datasource])
        this
        (do
          (log/info "Stopping database")
          (doseq [[key db-spec] db-specs]
            (hikari-cp/close-datasource (:datasource db-spec))
            (log/info "Closed datasource for" key))
          (assoc this
                 :db-specs (into
                            {} (map (fn [[key db-spec]]
                                      [key (dissoc db-spec :datasource)])
                                    db-specs)))))))

  IDatabase
  (cache-pages [db]
    ;; clear internal storage
    (let [stored-pages (internal/read-storage @storage :reverie/pages)]
      (doseq [serial stored-pages]
        (delete-stored-page serial false)
        (delete-stored-page serial true))
      (internal/delete-storage @storage :reverie/pages)

      ;; fill internal storage with the pages
      (doall
       ;; force evalutation so that the cache is filled
       (rev.db/get-pages db))
      nil))
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
      (db/with-transaction [db :default]
        (let [data (assoc data
                          :serial serial
                          :template (-> data :template kw->str)
                          :app (-> data :app kw->str)
                          :type (-> data :type kw->str)
                          :slug (or (:slug data)
                                    (slugify (:name data)))
                          :order (inc order)
                          :version 0)
              {:keys [id] :as page-data} (db/query! db sql-add-page<! data)]
          (recalculate-routes db id)
          (auth/add-authorization! (rev.db/get-page db id)
                                   db
                                   :all
                                   :view)
          (auth/add-authorization! (rev.db/get-page db id)
                                   db
                                   :edit
                                   :staff)
          (auth/add-authorization! (rev.db/get-page db id)
                                   db
                                   :add
                                   :staff)
          (rev.db/get-page db (:id page-data))))))

  (update-page! [db id data]
    (let [data (select-keys data [:template :name :title
                                  :route :type :app :slug])]
      (assert (not (empty? data)) "update-page! does not take an empty data set")
      (db/with-transaction [db :default]
        (db/query! db {:update :reverie_page
                       :set (assoc data :updated (sql/raw "now()"))
                       :where [:= :id id]})
        (recalculate-routes db id)
        (rev.db/get-page db id))))

  (save-page-properties! [db serial data]
    (db/with-transaction [db :default]
      (db/query! db {:delete-from :reverie_page_properties
                     :where [:and
                             [:in :key (vec (map name (keys data)))]
                             [:= :page_serial serial]]})
      (db/query! db {:insert-into :reverie_page_properties
                     :values (map (fn [[k v]]
                                    {:key (name k) :value v :page_serial serial})
                                  data)}))
    (do (delete-stored-page serial false)
        (delete-stored-page serial true)
        (rev.db/get-page db serial false)
        (rev.db/get-page db serial true)))

  (move-page! [db id origo-id movement]
    (let [movement (keyword movement)]
      (assert id "id has to be non-nil")
      (assert origo-id "origo-id has to be non-nil")
      (assert (some #(= % movement) [:after :before :over]) "movement has to be :after, :before or :over")
      (if (= movement :over)
        (let [new-parent
              (-> (db/query db {:select [:serial]
                                :from [:reverie_page]
                                :where [:= :id origo-id]})
                  first :serial)
              order
              (or
               (-> (db/query db {:select [:%max.p.order]
                                 :from [[:reverie_page :p]]
                                 :join [[:reverie_page :parent]
                                        [:= :parent.serial :p.parent]]
                                 :where [:and
                                         [:= :parent.id origo-id]
                                         [:= :parent.version 0]]})
                   first
                   :max)
               0)]
          (db/with-transaction [db :default]
            (db/query! db {:update :reverie_page
                           :set {(sql/raw "\"order\"") (inc order)
                                 :parent new-parent}
                           :where [:= :id id]})
            (recalculate-routes db id)))
        (let [parent-origo
              (-> (db/query db {:select [:parent]
                                :from [:reverie_page]
                                :where [:= :id origo-id]})
                  first :parent)
              parent
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
                                         [:<> :p.id id]
                                         [:= :p.version 0]
                                         [:= :o.id origo-id]]
                                 :order-by [(sql/raw "\"order\"")]})
                   (map (fn [{:keys [order id]}]
                          [order id])))
              objs
              (cond
                (= movement :after)
                (movement/after objs origo-id id)

                :else
                (movement/before objs origo-id id))]
          (db/with-transaction [db :default]
            (doseq [[order id] objs]
              (db/query! db {:update :reverie_page
                             :set {(sql/raw "\"order\"") order
                                   :parent parent-origo}
                             :where [:= :id id]}))
            (recalculate-routes db id))))
      (do (delete-stored-page id)
          (rev.db/get-page db id))))

  (add-object! [db data]
    (assert (contains? data :page_id) ":page_id key is missing")
    (assert (contains? data :area) ":area key is missing")
    (assert (contains? data :route) ":route key is missing")
    (assert (contains? data :name) ":name key is missing")
    (assert (contains? data :properties) ":properties key is missing")

    (db/with-transaction [db :default]
      (let [order (inc (or (-> (db/query db {:select [(sql/raw "max(\"order\")")]
                                             :from [:reverie_object]
                                             :where [:and
                                                     [:= :page_id (:page_id data)]
                                                     [:= :area (:area data)]]})
                               first
                               :max)
                           0))
            obj (db/query! db sql-add-object<! (-> data
                                                   (assoc :order order)
                                                   (dissoc :fields)))
            obj-meta (sys/object (-> data :name keyword))
            field-ks (->> obj-meta :options :fields keys)
            fk (or (->> obj-meta :options :foreign-key)
                   :object_id)
            table (get-in obj-meta [:options :table])
            obj-id (:id obj)
            properties (merge (object/initial-fields
                               (-> data :name keyword)
                               {:database db})
                              (select-keys (:properties data) field-ks)
                              {fk obj-id})]
        (db/query! db {:insert-into (sql/raw table)
                       :values [properties]})
        (do (delete-stored-page (:page_id data))
            (rev.db/get-page db (:page_id data)))

        obj)))

  (update-object! [db id data]
    (let [{obj-name :name page-id :page_id} (-> (db/query db {:select [:name :page_id]
                                                              :from [:reverie_object]
                                                              :where [:= :id id]})
                                                first)
          obj-name (keyword obj-name)
          obj-meta (sys/object obj-name)
          fk (or (->> obj-meta :options :foreign-key)
                 :object_id)
          field-ks (->> obj-meta :options :sections
                        (map (fn [{:keys [fields]}] fields)) flatten set)
          table (or (get-in obj-meta [:options :table])
                    obj-name)
          fields (merge (select-keys data field-ks))]
      (assert (not (empty? fields))
              "update-object! does not take an empty data set")
      (db/query! db {:update (sql/raw table)
                     :set fields
                     :where [:= fk id]})
      (do (delete-stored-page page-id)
          (rev.db/get-page db page-id))
      nil))

  (move-object!
    ([db id direction]
     (assert (some #(= % (keyword direction)) [:up :down :bottom :top])
             "direction has to be :up, :down, :bottom or :top")
     (let [{type :type page-id :id} (-> (db/query db {:select [:p.type :p.id]
                                                      :from [[:reverie_page :p]]
                                                      :join [[:reverie_object :o]
                                                             [:= :o.page_id :p.id]]
                                                      :where [:= :o.id id]})
                                        first)
           origo? (= type "app")
           objs (->
                 (map
                  (fn [{:keys [order id]}]
                    [order id])
                  (db/query db {:select [:o.order :o.id]
                                :from [[:reverie_object :f]]
                                :join [[:reverie_object :o]
                                       [:= :f.page_id :o.page_id]]
                                :where [:and
                                        [:= :f.id id]
                                        [:= :f.area :o.area]]
                                :order-by [:o.order]}))
                 (movement/move id direction origo?))]
       (db/with-transaction [db :default]
         (doseq [[order id] objs]
           (if-not (nil? id)
             (db/query! db {:update :reverie_object
                            :set {(sql/raw "\"order\"") order}
                            :where [:= :id id]}))))
       (do (delete-stored-page page-id)
           (rev.db/get-page db page-id))))
    ([db id page-id area]
     (let [area (kw->str area)
           order (inc (or (-> (db/query db {:select [:%max.o.order]
                                            :from [[:reverie_object :o]]
                                            :where [:and
                                                    [:= :o.page_id page-id]
                                                    [:= :o.area area]]})
                              first :max)
                          0))]
       (db/query! db {:update :reverie_object
                      :set {(sql/raw "\"order\"") order
                            :area area
                            :page_id page-id}
                      :where [:= :id id]})
       (do (delete-stored-page page-id)
           (rev.db/get-page db page-id)))))

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
      (db/with-transaction [db :default]
        (doseq [[order id] objs]
          (db/query! db {:update :reverie_object
                         :set {(sql/raw "\"order\"") order
                               :page_id page_id}
                         :where [:= :id id]})))
      (do (delete-stored-page page_id)
          (rev.db/get-page db id))))

  (get-pages
    ([db]
     (let [ids (mapv :id (db/query db {:select [:id]
                                       :from [:reverie_page]
                                       :where [:in :version [0 1]]}))
           pages (get-stored-pages ids)]
       (if-not (empty? pages)
         pages
         (map (partial get-page db)
              (db/query db sql-get-pages-1)))))
    ([db published?]
     (let [ids (mapv :id (db/query db {:select [:id]
                                       :from [:reverie_page]
                                       :where [:= :version (if published? 1 0)]}))
           pages (get-stored-pages ids)]
       (if-not (empty? pages)
         pages
         (map (partial get-page db)
              (db/query db sql-get-pages-2 {:version (if published? 1 0)}))))))

  (get-page-with-route [db serial]
    (map (fn [page-data]
           [(:route page-data) page-data])
         (map massage-page-data
              (db/query db {:select [:*]
                            :from [:reverie_page]
                            :where [:and
                                    [:= :serial serial]
                                    [:or
                                     [:= :version 0]
                                     [:= :version 1]]]}))))

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
  (get-page
    ([db id]
     (if-let [page (get-stored-page id)]
       page
       (get-page db (first (db/query db sql-get-page-1 {:id id})))))
    ([db serial published?]
     (if-let [page (get-stored-page serial published?)]
       page
       (get-page db (first (db/query db sql-get-page-2 {:serial serial
                                                        :version (if published? 1 0)}))))))
  (get-children
    ([db page]
     (let [ids (mapv :id (db/query db {:select [:id]
                                       :from [:reverie_page]
                                       :where [:and
                                               [:= :version (page/version page)]
                                               [:= :parent (page/serial page)]]}))
           children (get-stored-pages ids)]
       (if-not (empty? children)
         children
         (map (partial get-page db)
              (db/query db sql-get-page-children {:version (page/version page)
                                                  :parent (page/serial page)})))))
    ([db serial published?]
     (let [ids (mapv :id (db/query db {:select [:id]
                                       :from [:reverie_page]
                                       :where [:and
                                               [:= :version (if published? 1 0)]
                                               [:= :parent serial]]}))
           children (get-stored-pages ids)]
       (if-not (empty? children)
         children
         (map (partial get-page db)
              (db/query db sql-get-page-children {:version (if published? 1 0)
                                                  :parent serial}))))))
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

  (get-object [db id]
    ;; go through the page method instead because objects
    ;; are supposed to have the page record with all of the objects
    ;; associated with it
    (let [page-id (->> (db/query db {:select [:page_id]
                                     :from [:reverie_object]
                                     :where [:= :id id]})
                       first :page_id)
          page (rev.db/get-page db page-id)
          object (->> (page/objects page)
                      (filter #(= id (object/id %)))
                      first)]
      object))

  (get-objects [db page]
    (let [objs-meta
          (db/query db {:select [:*]
                        :from [:reverie_object]
                        :where [:and
                                [:= :page_id (page/id page)]
                                [:not= :version -1]
                                ;; only fetch obj-data that belongs
                                ;; to objects that are actually initialized
                                [:in :name (map kw->str (keys (:objects @sys/storage)))]]
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
                       :page page
                       :route (route/route [(:route obj-meta)])
                       :area (keyword (:area obj-meta))
                       :name (keyword (:name obj-meta))
                       :options (:options obj-data)
                       :methods (:methods obj-data)))))
           objs-meta)))

  IPublish

  (publish-page!
    ([db page-id]
     (publish/publish-page! db page-id false))
    ([db page-id recur?]
     (let [ ;; page-unpublished
           pu (rev.db/get-page db page-id)
           pages (if recur?
                   (mapv (fn [{:keys [serial]}]
                           (rev.db/get-page db serial false))
                         ;; get_serials_recursively includes the first serial
                         (db/query db (str "SELECT * FROM get_serials_recursively(" (page/serial pu) ");")))
                   [pu])]
       (db/with-transaction [db :default]
         (doseq [pu pages]
           (shift-versions! db (page/serial pu))
           (let [{:keys [id] :as copied} (db/query! db sql-copy-page<! {:id (page/id pu)})]
             (doseq [obj (page/objects pu)]
               (let [ ;; copy meta object
                     new-meta-obj (db/query! db sql-copy-object-meta<!
                                             {:pageid id
                                              :id (object/id obj)})
                     ;; get meta data for meta object
                     obj-meta (sys/object (-> new-meta-obj :name keyword))
                     ;; foreign key
                     fk (or (->> obj-meta :options :foreign-key)
                            :object_id)]
                 ;; check that the object has been loaded, or has not
                 ;; been disabled in defobject
                 (when obj-meta
                   (db/query! db {:insert-into (:table obj-meta)
                                  :values [(assoc (object/properties obj)
                                                  fk (:id new-meta-obj))]})))))
           (recalculate-routes db (page/id pu))
           (db/query! db sql-update-published-pages-order! {:parent (page/parent pu)})))
       (doseq [p pages]
         ;; first remove unpublished from cache and the refetch it
         ;; this is so that the unpublished page can properly see published? status
         (delete-stored-page (page/id p))
         (rev.db/get-page db (page/id p))
         ;; then remove and add published page so that it's updated
         (delete-stored-page (page/serial p) true)
         (rev.db/get-page db (page/serial p) true)))))

  (unpublish-page! [db page-id]
    (let [;; page-unpublished
          pu (rev.db/get-page db page-id)
          pages (mapv (fn [{:keys [serial]}]
                        (rev.db/get-page db serial false))
                      ;; get_serials_recursively includes the first serial
                      (db/query db (str "SELECT * FROM get_serials_recursively(" (page/serial pu) ");")))]
      ;; we shift all pages that are above unpublished up one step,
      ;; thus causing the published page to become unpublished as its
      ;; version number now becomes 2 (ie, it's in history)
      (doseq [pu pages]
        (shift-versions! db (page/serial pu))
        ;; remove published page from internal cache
        (delete-stored-page (page/serial pu) true)
        ;; remove unpublished page from cache
        (delete-stored-page (page/id pu))
        (rev.db/get-page db (page/id pu)))
      nil))

  (trash-page! [db page-id]
    (let [;; page-unpublished
          pu (rev.db/get-page db page-id)
          pages (mapv (fn [{:keys [serial]}]
                        (rev.db/get-page db serial false))
                      ;; get_serials_recursively includes the first serial
                      (db/query db (str "SELECT * FROM get_serials_recursively(" (page/serial pu) ");")))]
      ;; recursively trash all pages underneath
      (db/with-transaction [db :default]
        (doseq [pu pages]
          (shift-versions! db (page/serial pu))
          (db/query! db {:update :reverie_page
                         :set {:version -1}
                         :where [:= :id (page/id pu)]})
          (delete-stored-page (page/id pu))
          (delete-stored-page (page/serial pu) true)))))

  (trash-object! [db obj-id]
    (let [page-id (->> (db/query db {:select [:page_id]
                                     :from [:reverie_object]
                                     :where [:= :id obj-id]})
                       first :page_id)]
      (db/query! db {:update :reverie_object
                     :set {:version -1}
                     :where [:= :id obj-id]})
      (delete-stored-page page-id)
      (rev.db/get-page db page-id)
      nil))

  IUserDatabase
  (get-users [db]
    (map (fn [{:keys [id created username active_p email
                     spoken_name full_name last_login groups roles]}]
           (extend-user
            (auth/map->User
             {:id id :created created :active? active_p
              :username username :email email
              :spoken-name spoken_name :full-name full_name
              :last-login last_login
              :roles (get-user-roles roles groups)
              :groups (get-user-groups groups)})
            db))
         (db/query db sql-get-users)))

  (get-user
    ([db]
     (if-let [user-id (session/get :user-id)]
       (auth/get-user db user-id)))
    ([db id]
     (if-let [user (->> {:id id}
                        (db/query db (cond
                                       (and (string? id) (re-find #"@" id)) sql-get-user-by-email
                                       (string? id) sql-get-user-by-username
                                       (uuid? id) sql-get-user-by-token
                                       :else sql-get-user-by-id))
                        first)]
       (let [{:keys [id created username active_p email
                     spoken_name full_name last_login roles groups]} user]
         (extend-user
          (auth/map->User
           {:id id :created created
            :username username :email email
            :spoken-name spoken_name :full-name full_name
            :last-login last_login
            :active? active_p
            :roles (get-user-roles roles groups)
            :groups (get-user-groups groups)})
          db))))))

(s/defn ^:always-validate database
  ([settings :- {s/Any s/Any}]
   (db/map->EzDatabase settings))
  ([dev? :- s/Bool db-specs :- {s/Any s/Any}]
   (db/map->EzDatabase {:dev? dev? :db-specs db-specs :ds-specs {}}))
  ([dev? :- s/Bool db-specs :- {s/Any s/Any} ds-specs :- {s/Any s/Any}]
   (db/map->EzDatabase {:dev? dev? :db-specs db-specs :ds-specs ds-specs})))
