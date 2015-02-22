(ns reverie.modules.sql
  (:require [honeysql.core :as sql]
            [reverie.database :as db]
            [reverie.module :as module]
            [reverie.module.entity :as entity])
  (:import [reverie.module Module]))


(defn get-entity-table [entity]
  (get-in entity [:options :table]))

(defn get-m2m-tables [entity]
  (let [fields (get-in entity [:options :fields])]
    (reduce
     (fn [out [k {:keys [name type pk order table options m2m]}]]
       (assoc out k
              {:name name
               :type type
               :pk (or pk :id)
               :order (or order pk :id)
               :table table
               :options options
               :m2m m2m}))
     {}
     (filter
      (fn [[key data]]
        (= :m2m (:type data))) fields))))

(defn get-pk [entity]
  (or (get-in entity [:options :pk])
      :id))

(defn- get-m2m-data [db m2m]
  (reduce
   (fn [out [k {:keys [name pk order
                       table options m2m]}]]
     (assoc out k
            (db/query db {:select options
                          :from [table]
                          :order-by [order]})))
   {} (into [] m2m)))


(extend-type Module
  module/IModuleDatabase
  (get-data
    ([this entity id]
       (module/get-data this entity id nil))
    ([this entity id args]
       (let [db (:database this)
             table (get-entity-table entity)
             pk (get-pk entity)
             m2m (get-m2m-tables entity)

             ;; get data for the table
             data
             (first
              (db/query db (merge
                            args
                            {:select [:*]
                             :from [table]
                             :order-by [pk]
                             :where [:= pk id]})))

             ;; get data about m2m
             m2m-data (get-m2m-data db m2m)

             ;; get the data about the m2m joining tables
             shared-m2m-data
             (when-not (empty? data)
               (reduce
                (fn [out [k {:keys [m2m]}]]
                  (let [{:keys [table joining]} m2m
                        [this that] joining]
                    (assoc out k
                           (->>
                            ;; get the data
                            (db/query db {:select [that]
                                          :from [table]
                                          :where [:= this (get data pk)]})
                            (map that)
                            ;; pour it into a hash-map
                            (into #{})))))
                {} m2m))]
         {:entity-data (merge data shared-m2m-data)
          ;; the data for the m2m tables
          :m2m-data m2m-data})))

  (save-data [this entity id data]
    (let [db (:database this)
          table (get-entity-table entity)
          pk (get-pk entity)
          m2m (get-m2m-tables entity)]
      ;; update table
      (db/query! db {:update table
                     :set (apply dissoc data (keys m2m))
                     :where [:= pk id]})
      ;; loop through the m2m tables
      (doseq [[k {:keys [m2m]}] m2m]
        (let [{:keys [table joining]} m2m
              [this that] joining]
          ;; delete old m2m data
          (db/query! db {:delete-from table
                         :where [:= this id]})
          ;; reinsert any new m2m data
          (when-not (empty? (get data k))
            (db/query! db {:insert-into table
                           :values (map (fn [value]
                                          {this id that value})
                                        (get data k))}))))))

  (add-data [this entity data]
    (let [db (:database this)
          table (get-entity-table entity)
          pk (get-pk entity)
          m2m (get-m2m-tables entity)
          ;; add data and get the id
          id (->
              (db/query<! db {:insert-into table
                              :values [(apply dissoc data (keys m2m))]})
              first
              (get pk))]
      ;; add any m2m data
      (doseq [[k {:keys [m2m]}] m2m]
        (let [{:keys [table joining]} m2m
              [this that] joining]
          (when-not (empty? (get data k))
            (db/query! db {:insert-into table
                           :values (map (fn [value]
                                          {this id that value})
                                        (get data k))}))))))

  (delete-data
    ([this entity id]
       (module/delete-data this entity id false))
    ([this entity id cascade?]
       (let [db (:database this)
             table (get-entity-table entity)
             pk (get-pk entity)]
         (try
           ;; first try a sql based cascade if cascade? is true
           (db/query! db {:delete-from (if cascade?
                                         (sql/raw (str (name table)
                                                       " CASCADE"))
                                         (sql/raw (name table)))
                          :where [:= pk id]})
           ;; catch exception if a sql based cascade is not possible
           ;; and try and run a manual one
           (catch Exception e
             (when cascade?
               (let [m2m (get-m2m-tables entity)]
                 (doseq [[k {:keys [m2m]}] m2m]
                   (let [{:keys [table joining]} m2m
                         [this _] joining]
                     (db/query! db {:delete-from table
                                    :where [:= this id]})))))
             (db/query! db {:delete-from table
                            :where [:= pk id]})))))))
