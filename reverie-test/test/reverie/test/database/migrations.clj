(ns reverie.test.database.migrations
  (:require [joplin.core :as joplin]
            joplin.jdbc.database
            [midje.sweet :refer :all]))


(comment
  (let [mmap {:db {:type :sql
                   :migrations-table "migrations"
                   :url (str "jdbc:postgresql:"
                             "//localhost:5432/dev_reverie"
                             "?user=" "devuser"
                             "&password=" "devuser")}
              :migrator "resources/migrations/postgresql"}]
    (joplin/rollback-db mmap 1)
    (joplin/migrate-db mmap)))
