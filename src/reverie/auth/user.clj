(ns reverie.auth.user
  (:refer-clojure :exclude [get])
  (:require [noir.util.crypt :as crypt]
            [noir.session :as session]
            [noir.cookies :as cookies]
            [korma.core :as k])
  (:use reverie.entity))


(defn exists? [name]
  (not (nil? (first (k/select user (k/where {:name name}))))))

(defn admin?
  ([]
     (admin? (get)))
  ([user]
     (:is_admin user)))

(defn staff?
  ([]
     (staff? (get)))
  ([user]
     (:is_staff user)))

(defn gen-session []
  (let [salt (crypt/gen-salt)]
    (session/put! salt {:session-id salt})
    (cookies/put! :session salt)
    (session/get salt)))

(defn get-session []
  (if-let [session-id (cookies/get :session)]
    (if-let [sess (session/get session-id)]
      sess
      (gen-session))
    (gen-session)))

(defn logged-in? []
  (if (nil? (:user-id (get-session)))
    false
    true))


(defn get-id []
  (:user-id (get-session)))

(defn get 
  ([] (if-let [user-id (get-id)]
        (-> user (k/select (k/where {:id user-id})) first)))
  ([name]
     (-> user (k/select (k/where {:name name})) first))
  ([name active?]
     (-> user (k/select (k/where {:name name :active active?})) first)))


;; (defn get-roles []
;;   (k/select role (k/where {:user_id (get-id)})))

;; (defn has-role? [roles]
;;   (let [user-roles (set (get-roles connection))
;;         roles (set roles)]
;;     (= roles (cset/intersection roles user-roles))))

(defn login [name password]
  (if-let [user (get name true)]
    (if (and (not (= user nil)) (crypt/compare password (:password user)))
      (let [sess (get-session)]
        (session/put! (:session-id sess) (assoc sess :user-id (:id user)))
        true)
      false)
    :user-does-not-exist))

(defn logout []
  (session/clear!)
  (cookies/put! :session nil))

(defn add! [{:keys [first-name last-name
                    name email password
                    is-staff is-admin
                    roles groups]}]
  (if-not (exists? email)
    (let [is-staff (or is-staff false)
          is-admin (or is-admin false)]
      (k/insert user
                (k/values {:first_name first-name :last_name last-name
                           :name name
                           :email email :password (crypt/encrypt password)
                           :active true :is_staff is-staff :is_admin is-admin}))
      true)
    false))

(defn update-password
  ([password]
     (if-let [user-id (get-id)]
       (k/update (k/set-fields {:password (crypt/encrypt password)})
                 (k/where {:id user-id}))))
  ([name password]
     (if-let [user-id (:id (get name))]
       (k/update (k/set-fields {:password (crypt/encrypt password)})
                 (k/where {:id user-id})))))

(defn update
  ([data]
     (if-let [user-id (get-id)]
       (k/update (k/set-fields data)
                 (k/where {:id user-id}))))
  ([name data]
     (if-let [user-id (:id (get name))]
       (k/update (k/set-fields data)
                 (k/where {:id user-id})))))
