(ns reverie.test.security
  (:require [com.stuartsierra.component :as component]
            [reverie.auth :as auth]
            [reverie.database :as db]
            reverie.modules.auth
            [reverie.security :refer [with-access with-authorize]
             :as security]
            [reverie.test.database.sql-helpers :refer [get-db
                                                       seed!]]
            [slingshot.slingshot :refer [try+]]
            [midje.sweet :refer :all]))



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
      => :reverie.security/not-allowed)


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
      => :reverie.security/not-allowed)


(fact "with-authorized - user authorized"
      (try+
       (with-authorize {:roles #{:admin}} :view {:view #{:staff}
                                                 :edit #{:staff}}
         true)
       (catch Object o
         (:type o)))
      => true)


(fact "authorize?"
      (seed!)
      (let [db (component/start (get-db))]
        (try
          (let [p (db/get-page db 2)
                u (auth/get-user db 1)]
            (fact "view page"
                  (security/authorize? p u db :view)
                  => true)
            (fact "edit page"
                  (security/authorize? p u db :edit)
                  => false))
          (catch Exception e
            #spy/d e))
        (component/stop db)))
