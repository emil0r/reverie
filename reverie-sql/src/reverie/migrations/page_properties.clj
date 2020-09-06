(ns reverie.migrations.page-properties
  (:require [clojure.edn :as edn]
            [ez-database.core :as db]))

(defn migrate-up [config]
  (let [db (:ezdb config)
        properties (db/query db {:select [:page_serial
                                          #sql/raw "json_agg(json_build_object(key, value)) as data"]
                                 :from [:reverie_page_properties]
                                 :group-by [:page_serial]})]
    (db/with-transaction [db :default]
      (db/query! db {:delete-from :reverie_page_properties})
      (let [values (map (fn [{:keys [page_serial data]}]
                          (let [merged-data (apply merge data)
                                new-data (reduce (fn [out [k v]]
                                                   (assoc out k (edn/read-string v)))
                                                 {} merged-data)]
                            {:page_serial page_serial
                             :data (reverie.database.sql/->JSONB new-data)
                             :key ""
                             :value ""}))
                        properties)]
        (when-not (empty? values)
          (db/query! db {:insert-into :reverie_page_properties
                         :values values}))))))

(defn migrate-down [config]
  (let [db (:ezdb config)
        properties (->> {:select [:page_serial :data]
                         :from [:reverie_page_properties]}
                        (db/query db)
                        (map (fn [{:keys [page_serial data]}]
                               (map (fn [[k v]]
                                      {:page_serial page_serial
                                       :key (name k)
                                       :value (pr-str v)
                                       :data (reverie.database.sql/->JSONB {})})
                                    data)))
                        (flatten))]
    (db/with-transaction [db :default]
      (db/query! db {:delete-from :reverie_page_properties})
      (when-not (empty? properties)
        (db/query! db {:insert-into :reverie_page_properties
                       :values properties})))))
