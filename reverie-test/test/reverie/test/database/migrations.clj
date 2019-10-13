(ns reverie.test.database.migrations
  (:require [migratus.core :as migratus]
            [midje.sweet :refer :all]))


(comment

  (let [mmap {:store :database
              :migration-dir "migrations/reverie/postgresql"
              :migration-table-name "migrations"
              :db {:classname "org.postgresql.Driver"
                   :subprotocol "postgresql"
                   :subname "//localhost:5432/dev_reverie"
                   :user "devuser"
                   :password "devuser"}}]
    (migratus/down mmap 2 1)
    (migratus/migrate mmap))
  )
