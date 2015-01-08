(ns reverie.test.database.migrations
  (:require [joplin.core :as joplin]
            joplin.jdbc.database
            [midje.sweet :refer :all]))



(let [jmap {:db {:type :sql
                 :url (str "jdbc:postgresql:"
                           "//localhost:5432/dev_reverie"
                           "?user=" "devuser"
                           "&password=" "devuser")}
            :migrator (str "resources/migrations/postgresql")}]
  (joplin/rollback-db jmap)
  (joplin/migrate-db jmap))
