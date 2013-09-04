(ns reveriecms.init
  (:require [lobos.core :as lobos]
            [lobos.migrations :as migrations]
            [reverie.auth.user :as user]
            [reverie.server :as server]
            reveriecms.templates.main)
  (:use [korma.db :only [defdb postgres]]
        [reverie.atoms :only [read-routes!]]))

(defdb reveriecms-db (postgres {:db "reveriecms"
                                :user "reveriecms"
                                :password "reveriecms"}))


(def lobos-db {:classname "org.postgresql.Driver"
               :subprotocol "postgresql"
               :subname "//localhost:5432/reveriecms"
               :user "reveriecms"
               :password "reveriecms"})

(defn init-db []
  (user/add! {:first-name "Admin" :last-name "Admin" :name "admin"
              :password "admin0r" :email "" :is-staff true :is-admin true}))

(defn init []
  (migrations/open-global-when-necessary lobos-db)
  (lobos.core/migrate)
  (read-routes!)
  (server/load-views "templates"))

;;(init)
;;(init-db)

