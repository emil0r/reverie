(ns reverie.test.auth
  (:require [com.stuartsierra.component :as component]
            [reverie.auth :as auth :refer [with-access with-authorize]]
            [reverie.database :as db]
            [reverie.nsloader]
            [reverie.test.database.sql-helpers :refer [get-db seed!]]
            [slingshot.slingshot :refer [try+]]
            [taoensso.timbre :as log]
            [midje.sweet :refer :all]))


(fact
 (let [db (component/start (get-db))]
   (try
     (seed! db)
     (try
       (fact "login"
             (auth/login {:username "admin"
                          :password "admin"} db)
             (auth/logged-in?)
             => true)
       (fact "get user"
             (auth/login {:username "admin"
                          :password "admin"} db)
             (:username (auth/get-user db 1))
             => "admin")
       (fact "logout"
             (auth/logout)
             (auth/logged-in?)
             => false)
       (catch Exception e
         (log/error e)))
     (finally (component/stop db)))))


(fact "with-access - nil nil"
      (with-access nil nil true)
      => true)


(fact "with-access - user not allowed"
      (try+
       (with-access {:roles #{:deny-me}} {:view #{:admin :staff}
                                          :edit #{:admin :staff}}
         true)
       (catch Object o
         (:type o)))
      => :reverie.auth/not-allowed)


(fact "with-access - :all, anyone allowed"
      (try+
       (with-access nil {:view #{:all}
                         :edit #{:admin :staff}}
         true)
       (catch Object o
         (:type o)))
      => true)


(fact "with-access - user accessed"
      (try+
       (with-access {:roles #{:admin}} {:view #{:staff}
                                        :edit #{:staff}}
         true)
       (catch Object o
         (:type o)))
      => true)


(fact "with-authorize - user denied"
      (try+
       (with-authorize {:roles #{:deny-me}} :view {:view #{:admin :staff}
                                                   :edit #{:admin :staff}}
         true)
       (catch Object o
         (:type o)))
      => :reverie.auth/not-allowed)


(fact "with-authorized - user authorized"
      (try+
       (with-authorize {:roles #{:admin}} :view {:view #{:staff}
                                                 :edit #{:staff}}
         true)
       (catch Object o
         (:type o)))
      => true)


(fact
 "authorize?"
 (let [db (component/start (get-db))]
   (try
     (seed! db)
     (let [p (db/get-page db 1 false)
           u (auth/get-user db 1)]
       (fact "view page"
             (auth/authorize? p u db :view)
             => true)
       (fact "edit page"
             (auth/authorize? p u db :edit)
             => true))
     (catch Exception e
       (println e))
     (finally (component/stop db)))))
