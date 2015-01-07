(ns reverie.test.database.sql
  (:require [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [joplin.core :as joplin]
            joplin.jdbc.database
            [reverie.database :as db]
            [reverie.database.sql :as sql]
            [midje.sweet :refer :all]))

(def db-spec {:classname "org.postgresql.Driver"
              :subprotocol "postgresql"
              :subname "//localhost:5432/dev_reverie"
              :user "devuser"
              :password "devuser"})

(def db-spec-two
  {:classname "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname "//localhost:5432/dev_reverie"
   :user "devuser"
   :password "devuser"})

(let [db (component/start (sql/database {:default db-spec
                                         :two db-spec-two}))]
  (try
    (println (db/query db {:select [6]}))
    (println (db/query db :two {:select [:*] :from [:reverie_page] :limit 1}))
    (catch Exception e
      (println e)))
  (component/stop db))
