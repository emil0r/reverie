(ns reverie.cache.sql
  (:require [clj-time.core :as t]
            [reverie.cache :as cache]
            [reverie.database :as db]
            [reverie.page :as page]
            [reverie.time :as time]))

(defrecord BasicStrategy []
  cache/IPruneStrategy
  (prune! [this cachemanager]
    (let [{db :database} cachemanager
          pages (->> (db/query db {:select [:page_serial :value]
                                   :from [:reverie_page_properties]
                                   :where [:and
                                           [:not= :value ""]
                                           [:= :key "cache_clear_time"]]})
                     (map (fn [{:keys [page_serial value]}]
                            [page_serial (Integer/parseInt value)])))
          minute (Integer/parseInt (time/format (t/now) "mm"))]
      (doseq [[serial value] pages]
        (when (zero? (mod minute value))
          (cache/evict! cachemanager (page/page {:serial serial})))))))

(defn get-basicstrategy []
  (BasicStrategy.))
