(ns reverie.test.database.migrations
  (:require [migratus.core :as migratus]
            [midje.sweet :refer :all]
            [reverie.test.database.sql-helpers :refer [db-spec]]))


(comment

  (let [mmap {:store :database
              :migration-dir "migrations/reverie/postgresql"
              :migration-table-name "migrations"
              :db db-spec}]
    (migratus/down mmap 2 1)
    (migratus/migrate mmap))
  )
