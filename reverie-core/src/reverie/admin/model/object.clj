(ns reverie.admin.model.object
  (:require [clojure.walk :as walk]
            [ez-database.core :as db]
            [honeysql.core :as sql]
            [reverie.system :as sys]
            [reverie.util :refer [kw->str str->kw]]))


(defn get-objects
  "Taken straight from the reverie database protocol implementation. Modified for web usage"
  [db page-id]
  ;; only get objects if they exist
  (when-not (empty? (:objects @sys/storage))
    (let [objs-meta (db/query db {:select [:*]
                                  :from [:reverie_object]
                                  :where [:and
                                          [:= :page_id page-id]
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

      

      (map (fn [{:keys [id page_id updated created order route version] :as obj-meta}]
             (let [obj-data (get-in @sys/storage
                                    [:objects (keyword (:name obj-meta))])]
               {:object/id id
                :page/id page_id
                :object/area (keyword (:area obj-meta))
                :object/name (keyword (:name obj-meta))
                :object/properties (get objs-properties id)
                :object/updated updated
                :object/created created
                :object/order order
                :object/version version}))
           objs-meta))))

(defn- check-if-datasource [v]
  (if (fn? v)
    :reverie/datasource
    v))
(defn- assoc-datasources [fields]
  (->> fields
       (map (fn [[k v]]
              [k (walk/postwalk check-if-datasource v)]))
       (into {})))

(defn get-available-objects
  "Grab available objects. Takes a context (for now unused). Will give back meta data on all available objects."
  [_context]
  (->> (:objects @sys/storage)
       (map (fn [[obj-key {:keys [options] :as _object}]]
              {:object/name obj-key
               :object/fields (assoc-datasources (:fields options))
               :object/sections (:sections options)}))))

(defn get-datasource [db object-name object-field datasource-name]
  (if-let [ds (get-in @sys/storage [:objects object-name :options :fields object-field datasource-name])]
    (if (fn? ds)
      (ds {:database db}))))


(comment
  (let [db (:database @reverie.server/system)]
    (get-objects db 1))

  (get-available-objects nil)

  (let [db (:database @reverie.server/system)]
    (get-datasource db :reverie/faq :type :options))
  )
