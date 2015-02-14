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
  ;;#spy/d (msql/get-m2m-tables ent)
  (try
    ;;#spy/d (module/get-data mod ent {:where [:= :username "admin"]} 0 100)
    ;; #spy/d (module/save-data mod ent 1 {:full_name "Emil Bengtsson"
    ;;                                     :spoken_name "Emil"
    ;;                                     :email "emil@emil0r.com"
    ;;                                     :roles [1] :groups [1]})
    (module/add-data mod ent {:username "emil" :password "foo"
                              :spoken_name "Emil"
                              :full_name "Emil Bengtsson"
                              :email "emil@emil0r.com"
                              :roles [1]
                              :groups [1]})
    (catch Exception e
      (println e)))
  (component/stop db))
