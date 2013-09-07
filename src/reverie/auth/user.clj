(ns reverie.auth.user
  (:refer-clojure :exclude [get])
  (:require [clojure.set :as cset]
            [noir.util.crypt :as crypt]
            [noir.session :as session]
            [noir.cookies :as cookies]
            [korma.core :as k])
  (:use reverie.entity))


(defn exists? [name]
  (not (nil? (first (k/select user (k/where {:name name}))))))

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

(defn- get-roles [u]
  (into
   #{}
   (map
    #(keyword (:name %))
    (k/union
     (k/queries
      
      (-> role (k/subselect
                (k/fields [:name])
                (k/join :role_user
                        (= :role_user.role_id :role.id))
                (k/where {:role_user.user_id (:id u)
                          :active true})))

      (-> role (k/subselect
                (k/fields [:name])
                (k/join :role_group (= :role_group.role_id :role.id))
                (k/where {:role_group.group_id
                          [in (k/subselect
                               group
                               (k/fields :id)
                               (k/join :user_group
                                       (= :user_group.group_id :group.id))
                               (k/where {:user_group.user_id (:id u)
                                         :active true}))]}))))))))

(defn get 
  ([] (if-let [user-id (get-id)]
        (let [u (-> user (k/select (k/where {:id user-id})) first)]
          (assoc u :roles (get-roles u)))))
  ([name]
     (let [u (-> user (k/select (k/where {:name name})) first)]
       (assoc u :roles (get-roles u)))
     )
  ([name active?]
     (let [u (-> user (k/select (k/where {:name name :active active?})) first)]
       (assoc u :roles (get-roles u)))))


(defn role?
  "Check if the user has the role. Accepts oney keyword or a sequence of keywords. Each keyword representing a role"
  [user role-s]
  (if (keyword? role-s)
    (cset/subset? #{role-s} (:roles user))
    (cset/subset? (set role-s) (:roles user))))

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
