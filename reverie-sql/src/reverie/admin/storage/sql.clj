(ns reverie.admin.storage.sql
  (:require [clojure.edn :as edn]
            [ez-database.core :as db]
            [reverie.admin.storage :as admin.storage]
            [reverie.database.sql :as db.sql]
            [reverie.util :as util]
            [slingshot.slingshot :refer [try+]]
            [taoensso.timbre :as log])
  (:import [ez_database.core EzDatabase]))

(extend-type EzDatabase
  admin.storage/IAdminStorage
  (assoc! [db k v]
    (try+
     (db/query! db {:insert-into :admin_storage
                    :values [{:k (util/kw->str k) :v (pr-str v)}]})
     (catch [:type :ez-database.core/try-query] _
       (try+
        (db/query! db {:update :admin_storage
                       :set {:v (pr-str v)}
                       :where [:= :k (util/kw->str k)]})
        (catch [:type :ez-database.core/try-query] _
          ;; do nothing
          )))
     (catch Object _
       (log/error (:throwable &throw-context) "unexpected error"))))
  (dissoc! [db k]
    (db/query! db {:delete-from :admin_storage
                   :where [:= :k (util/kw->str k)]}))
  (get [db k]
    (try
      (->> (db/query db {:select [:v]
                         :from [:admin_storage]
                         :where [:= :k (util/kw->str k)]})
           first :v edn/read-string)
      (catch Exception _
        {}))))
