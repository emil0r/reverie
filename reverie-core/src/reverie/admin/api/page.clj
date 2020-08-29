(ns reverie.admin.api.page
  (:require [clojure.core.match :refer [match]]
            [clojure.edn :as edn]
            [reverie.database :as reverie.db]
            [reverie.page :as page]))


(defn get-pages [{{db :database} :reverie} page {:keys [id]}]
  (let [root (reverie.db/get-page db (or id 1) false)
        result (assoc root :children (page/children root db))]
    {:status 200
     :body {:result :success
            :payload result}}))
