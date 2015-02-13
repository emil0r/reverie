(ns reverie.test.modules.interface
  (:require [com.stuartsierra.component :as component]
            reverie.auth
            [reverie.database :as database]
            [reverie.module :as module]
            reverie.modules.auth
            [reverie.modules.sql :as msql]
            [reverie.system :as sys]
            [reverie.test.database.sql-helpers :refer [seed! get-db]]
            [midje.sweet :refer :all]))



(let [db (component/start (get-db))
      mod (assoc (-> @sys/storage :modules :auth :module)
            :database db)
      ent (module/get-entity mod "user")]
  ;;(msql/get-m2m-tables ent)
  (try
    #spy/d (module/get-data mod ent {:where [:= :username "admin"]} 0 100)
    (catch Exception e
      (println e)))
  (component/stop db))
