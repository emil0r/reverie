(ns reverie.test.database.sql-helpers
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [joplin.core :as joplin]
            joplin.jdbc.database
            reverie.sql.objects.text
            reverie.sql.objects.image
            [reverie.database :as db]
            [reverie.database.sql :as sql]
            [reverie.system :as sys]))



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

(defn get-db []
  (assoc (sql/database {:default db-spec
                        :two db-spec-two})
    :system (component/start (sys/map->ReverieSystem {}))))


(defn seed! []
  (let [jmaps (map (fn [[table path]]
                     {:db {:type :sql
                           :migration-table table
                           :url (str "jdbc:postgresql:"
                                     "//localhost:5432/dev_reverie"
                                     "?user=" "devuser"
                                     "&password=" "devuser")}
                      :migrator path})
                   (array-map
                    "ragtime_migrations_auth" "src/reverie/modules/migrations/auth/"
                    "ragtime_migrations_reverie_text" "src/reverie/sql/objects/migrations/text/"
                    "ragtime_migrations_reverie_image" "src/reverie/sql/objects/migrations/image/"
                    nil "resources/migrations/postgresql"))]
    (doseq [jmap jmaps]
      (joplin/rollback-db jmap 9000)))
  (let [db (component/start (get-db))
        seed (slurp (io/resource "seeds/postgresql/seed.sql"))]
    (try
      (doseq [line (str/split-lines seed)]
        (if-not (.startsWith line "--")
          (db/query! db line)))
      (catch Exception e
        (println e)))
    (component/stop db)))

(seed!)
