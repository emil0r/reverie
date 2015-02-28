(ns reverie.auth
  (:require [clojure.set :as set]
            [noir.session :as session]
            [slingshot.slingshot :refer [throw+]]))


(defrecord User [id username email
                 created last-login
                 spoken-name full-name
                 roles groups])


(defprotocol IUserDatabase
  (get-users [db])
  (get-user [db] [db id-or-email]))

(defprotocol IUserLogin
  (login [data db]))


(defn logged-in? []
  (not (nil? (session/get :user-id))))

(defn logout []
  (session/clear!)
  true)


;; authorization

(defmacro with-access [user required-roles & body]
  `(let [~'roles (apply set/union (vals ~required-roles))]
     (if (or (contains? (:roles ~user) :admin)
             (empty? ~'roles)
             (contains? ~'roles :all)
             (not (empty? (set/intersection
                           (:roles ~user) ~'roles))))
       ~@body
       (throw+ {:type ::not-allowed}))))


(defmacro with-authorize [user action required-roles & body]
  `(if (or (contains? (:roles ~user) :admin)
           (empty? ~required-roles)
           (contains? (get ~required-roles ~action) :all)
           (not (empty? (set/intersection
                         (:roles ~user)
                         (set/union
                          #{:all}
                          (get ~required-roles ~action))))))
     ~@body
     (throw+ {:type ::not-allowed})))


(defprotocol IAuthorize
  (authorize? [what user database action])
  (add-authorization! [what database role action])
  (remove-authorization! [what database role action]))
