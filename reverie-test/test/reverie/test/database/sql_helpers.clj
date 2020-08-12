(ns reverie.test.database.sql-helpers
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [ez-database.core :as db]
            [migratus.core :as migratus]
            [reverie.modules.auth]
            [reverie.batteries.objects.text]
            [reverie.batteries.objects.image]
            [reverie.database.sql :as sql]
            [reverie.system :as sys]
            [taoensso.timbre :as log]))



(def db-spec {:classname "org.postgresql.Driver"
              :subprotocol "postgresql"
              :subname "//localhost:15432/test"
              :user "devuser"
              :password "devuser"})
(def ds-spec {:maximum-pool-size 3
              :adapter "postgresql"
              :username "devuser"
              :port-number 15432
              :password "devuser"
              :database-name "test"})

(def db-spec-two
  {:classname "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname "//localhost:15432/test"
   :user "devuser"
   :password "devuser"})
(def ds-spec-two
  {:maximum-pool-size 3
   :adapter "postgresql"
   :username "devuser"
   :port-number 15432
   :password "devuser"
   :database-name "test"})

(defn get-db []
  (sql/database true {:default db-spec :two db-spec-two} {:default ds-spec :two ds-spec-two}))

(defn start-db []
  (component/start (get-db)))

(defn stop-db [database]
  (component/stop database))


(defn seed!
  ([]
   (let [db (component/start (get-db))]
     (try
       (seed! db)
       (finally
         (component/stop db)))))
  ([db]
   (with-out-str
     (let [seed (slurp (io/resource "seeds/postgresql/seed.sql"))
           mmaps (map (fn [[table path]]
                        {:store :database
                         :migration-dir path
                         :migration-table-name table
                         :db db-spec})
                      (array-map
                       "migrations_auth" "migrations/modules/auth/"
                       "migrations_reverie_text" "reverie/batteries/objects/migrations/text/"
                       "migrations_reverie_raw" "reverie/batteries/objects/migrations/raw/"
                       "migrations_reverie_image" "reverie/batteries/objects/migrations/image/"
                       "migrations_reverie" "migrations/reverie/postgresql/"
                       ))]
       (doseq [mmap mmaps]
         (log/debug "Rolling back" mmap)
         (migratus/down mmap 6 5 4 3 2 1))
       (doseq [mmap (reverse mmaps)]
         (log/debug "Migrating" mmap)
         (migratus/migrate mmap))
       (try
         (doseq [line (str/split-lines seed)]
           (if-not (.startsWith line "--")
             (db/query! db line)))
         (catch Exception e
           (log/error e)))))))



(comment
  ;; re-seed!
  (seed!)

  (def db (component/start (get-db)))

  (db/query db {:select [:*] :from [:reverie_page]})

  (db/query db "select * from migrations")
  (component/stop db)
  )
