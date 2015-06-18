(ns reverie.admin.storage.sql
  (:require [clojure.edn :as edn]
            [reverie.admin.storage :as admin.storage]
            [reverie.database :as db]
            [reverie.database.sql :as db.sql]
            [reverie.util :as util]
            [slingshot.slingshot :refer [try+]]
            [taoensso.timbre :as log])
  (:import [reverie.database.sql DatabaseSQL]))

(extend-type DatabaseSQL
  admin.storage/IAdminStorage
  (assoc! [db k v]
    (try+
     (db/query! db {:insert-into :admin_storage
                    :values [{:k (util/kw->str k) :v (pr-str v)}]})
     (catch [:type :reverie.database.sql/try-query] _
       (try+
        (db/query! db {:update :admin_storage
                       :set {:v (pr-str v)}
                       :where [:= :k (util/kw->str k)]})
        (catch [:type :reverie.database.sql/try-query] _
          ;; do nothing
          )))
     (catch Object _
       (log/error (:throwable &throw-context) "unexpected error"))))
  (dissoc! [db k]
    (db/query! db {:delete-from :admin_storage
                   :where [:= :k (util/kw->str k)]}))
  (get [db k]
    (->> (db/query db {:select [:v]
                       :from [:admin_storage]
                       :where [:= :k (util/kw->str k)]})
         first :v edn/read-string)))
