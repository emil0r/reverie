(ns reverie.test.auth
  (:require [com.stuartsierra.component :as component]
            [noir.session :as session]
            [noir.cookies :as cookies]
            [reverie.auth :as auth]
            [reverie.test.database.sql-helpers :refer [get-db seed!]]
            [reverie.test.helpers :refer [with-noir]]
            [midje.sweet :refer :all]))


(fact
 (let [db (component/start (get-db))]
   (with-noir
     (try
       (fact "login"
             (auth/login db "admin" "admin")
             (auth/logged-in?)
             => true)
       (fact "get user"
             (auth/login db "admin" "admin")
             (:username (auth/get-user db))
             => "admin")
       (fact "logout"
             (auth/logout)
             (auth/logged-in?)
             => false)
       (catch Exception e
         (println e))))
   (component/stop db)))
