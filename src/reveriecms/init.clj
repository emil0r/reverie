(ns reveriecms.init
  (:require [lobos.core :as lobos]
            [lobos.migrations :as migrations]
            [reverie.server :as server])
  (:use [korma.db :only [defdb postgres]]))

(defdb reveriecms-db (postgres {:db "reveriecms"
                                :user "reveriecms"
                                :password "reveriecms"}))


(def lobos-db {:classname "org.postgresql.Driver"
               :subprotocol "postgresql"
               :subname "//localhost:5432/reveriecms"
               :user "reveriecms"
               :password "reveriecms"})

(defn init []
  (migrations/open-global-when-necessary lobos-db)
  (lobos.core/migrate)
  (server/load-views "admin"))

;;(init)

