(ns reverie.admin.api.page
  (:require [reverie.admin.model.page :as model.page]))



(defn get-pages [{{db :database} :reverie} page params]
  #_(let [pages (db/query db
                        ^:opts {:post [:page :reverie/page]}
                        {:select [:*]
                         :from [:view_reverie_page]
                         :where [:= :version 0]})]
    {:status 200
     :body {:result :success
            :payload pages}}))
