(ns reverie.auth
  (:require [clojure.set :as set]
            [noir.session :as session]
            [slingshot.slingshot :refer [throw+]]))


(defrecord User [id username email active?
                 created last-login
                 spoken-name full-name
                 roles groups])

(def ^:dynamic *extended* "Extended data" {})

(defn extend!
  "Extend extensions to User record"
  [k v]
  (alter-var-root
   #'*extended*
   (fn [xt]
     (assoc xt k v))))
(defn retract!
  "Rectract extensions to User record"
  [k]
  (alter-var-root
   #'*extended*
   (fn [xt]
     (dissoc xt k))))

(defprotocol IUserDatabase
  (get-users [db] "Get all users")
  (get-user [db] [db id-or-email] "Get user by session or by id/email"))

(defprotocol IUserLogin
  (login [data db]))

(defprotocol IUserUpdate
  (update! [user data db])
  (set-password! [user new-password db]))

(defprotocol IUserToken
  (enable-token [id db] [id minutes db])
  (retire-token [id db])
  (expired-token? [id db]))

(defprotocol IUserAdd
  (add-user! [data roles groups db]))

(defn logged-in? []
  (not (nil? (session/get :user-id))))

(defn get-id []
  (session/get :user-id))

(defn logout []
  (session/clear!)
  true)


;; roles

(defmulti role? (fn [_ role-or-roles] (type role-or-roles)))

(defmethod role? clojure.lang.PersistentArrayMap [user roles]
  ;; the keys in roles correspond to roles
  ;; the values correspond to whether the user should have the role or not
  (every? true? (reduce (fn [out [k v]]
                          (if (true? v)
                            (conj out (contains? (:roles user) k))
                            (conj out (not (contains? (:roles user) k)))))
                        [] roles)))

(defmethod role? clojure.lang.Keyword [user role]
  ;; return true if the user has the role
  (contains? (:roles user) role))

(defmethod role? :default [user roles]
  ;; return true if the user have any of the roles
  (not (empty? (set/intersection (:roles user) (set roles)))))

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
