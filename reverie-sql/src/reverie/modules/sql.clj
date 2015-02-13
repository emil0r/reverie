(ns reverie.modules.sql
  (:require [reverie.database :as db]
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


(extend-type Module
  module/IModuleDatabase
  (get-data
    ([this entity offset limit]
       (module/get-data this entity nil offset limit))
    ([this entity args offset limit]
       (let [db (:database this)
             table (get-entity-table entity)
             pk (get-pk entity)
             m2m (get-m2m-tables entity)

             data
             (db/query db (merge
                           args
                           {:select [:*]
                            :from [table]
                            :order-by [pk]
                            :offset offset
                            :limit limit}))

             m2m-data
             (reduce
              (fn [out [k {:keys [name pk order
                                  table options m2m]}]]
                (assoc out k
                       (db/query db {:select options
                                     :from [table]
                                     :order-by [order]})))
              {} (into [] m2m))

             shared-m2m-data
             (when-not (empty? data)
               (reduce
                (fn [out [k {:keys [m2m]}]]
                  (let [{:keys [table joining]} m2m
                        [this that] joining]
                    (assoc out k
                           (->> (db/query db {:select [this that]
                                              :from [table]
                                              :where [:in this (map pk data)]})
                                (group-by this)
                                (map (fn [[k data]]
                                       {k (map this data)}))
                                (into {})))))
                {} m2m))]
         {:data (map (fn [data]
                       (let [id (get data pk)]
                         {:data data
                          :joins (into
                                  {}
                                  (map (fn [[k d]]
                                         {k (get d id)})
                                       shared-m2m-data))}))
                     data)
          :m2m-data m2m-data}))))
