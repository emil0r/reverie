(ns reverie.auth.sql
  (:require [buddy.hashers :as hashers]
            [clj-time.core :as t]
            [clj-time.local :as t.local]
            [clojure.string :as str]
            [ez-database.core :as db]
            [noir.session :as session]
            [reverie.auth :as auth]
            [reverie.module :as m]
            [reverie.page :as page]
            [reverie.auth :refer [IAuthorize
                                  IUserAdd
                                  IUserLogin
                                  IUserUpdate
                                  IUserToken]]
            [reverie.util :as util]
            [taoensso.timbre :as log])
  (:import [reverie.auth
            User]
           [reverie.page
            AppPage
            Page
            RawPage]
           [reverie.module
            Module]))

(extend-type Page
  IAuthorize
  (authorize? [page user db action]
    (or
     (contains? (:roles user) :admin)
     (let [row (-> (db/query db {:select [:*]
                                 :from [:auth_storage]
                                 :where [:and
                                         [:= :what "reverie.page/Page"]
                                         [:= :id_int (page/serial page)]
                                         [:in :role (map util/kw->str (:roles user))]
                                         [:= :action (util/kw->str action)]]})
                   first)]
       (not (nil? row)))))
  (add-authorization! [page db role action]
    (try
      (db/query! db {:insert-into :auth_storage
                     :values [{:what "reverie.page/Page"
                               :id_int (page/serial page)
                               :role (util/kw->str role)
                               :action (util/kw->str action)}]})
      (catch Exception _)))
  (remove-authorization! [page db role action]
    (db/query! db {:delete-from :auth_storage
                   :where [:and
                           [:= :what "reverie.page/Page"]
                           [:= :id_int (page/serial page)]
                           [:= :role (util/kw->str role)]
                           [:= :action (util/kw->str action)]]})))

(extend-type AppPage
  IAuthorize
  (authorize? [page user db action]
    (or
     (contains? (:roles user) :admin)
     (let [row (-> (db/query db {:select [:*]
                                 :from [:auth_storage]
                                 :where [:and
                                         [:= :what "reverie.page/AppPage"]
                                         [:= :id_int (page/serial page)]
                                         [:in :role (map util/kw->str (:roles user))]
                                         [:= :action (util/kw->str action)]]})
                   first)]
       (not (nil? row)))))
  (add-authorization! [page db role action]
    (try
      (db/query! db {:insert-into :auth_storage
                     :values [{:what "reverie.page/AppPage"
                               :id_int (page/serial page)
                               :role (util/kw->str role)
                               :action (util/kw->str action)}]})
      (catch Exception _)))
  (remove-authorization! [page db role action]
    (db/query! db {:delete-from :auth_storage
                   :where [:and
                           [:= :what "reverie.page/AppPage"]
                           [:= :id_int (page/serial page)]
                           [:= :role (util/kw->str role)]
                           [:= :action (util/kw->str action)]]})))

(extend-type RawPage
  IAuthorize
  (authorize? [page user db action]
    (or
     (contains? (:roles user) :admin)
     (let [row (-> (db/query db {:select [:*]
                                 :from [:auth_storage]
                                 :where [:and
                                         [:= :what "reverie.page/RawPage"]
                                         [:= :id_string (page/path page)]
                                         [:in :role (map util/kw->str (:roles user))]
                                         [:= :action (util/kw->str action)]]})
                   first)]
       (not (nil? row)))))
  (add-authorization! [page db role action]
    (try
      (db/query! db {:insert-into :auth_storage
                     :values [{:what "reverie.page/RawPage"
                               :id_string (page/path page)
                               :role (util/kw->str role)
                               :action (util/kw->str action)}]})
      (catch Exception _)))
  (remove-authorization! [page db role action]
    (db/query! db {:delete-from :auth_storage
                   :where [:and
                           [:= :what "reverie.page/RawPage"]
                           [:= :id_string (page/path page)]
                           [:= :role (util/kw->str role)]
                           [:= :action (util/kw->str action)]]})))

(extend-type Module
  IAuthorize
  (authorize? [module user db action]
    (or
     (contains? (:roles user) :admin)
     (let [row (-> (db/query db {:select [:*]
                                 :from [:auth_storage]
                                 :where [:and
                                         [:= :what "reverie.module/Module"]
                                         [:= :id_string (m/name module)]
                                         [:in :role (map util/kw->str (:roles user))]
                                         [:= :action (util/kw->str action)]]})
                   first)]
       (not (nil? row)))))
  (add-authorization! [module db role action]
    (try
      (db/query! db {:insert-into :auth_storage
                     :values [{:what "reverie.module/Module"
                               :id_string (m/name module)
                               :role (util/kw->str role)
                               :action (util/kw->str action)}]})
      (catch Exception _)))
  (remove-authorization! [module db role action]
    (db/query! db {:delete-from :auth_storage
                   :where [:and
                           [:= :what "reverie.module/Module"]
                           [:= :id_string (m/name module)]
                           [:= :role (util/kw->str role)]
                           [:= :action (util/kw->str action)]]})))


(extend-type clojure.lang.PersistentArrayMap
  IUserAdd
  (add-user! [data roles groups db]
    (if (->> (db/query db {:select [:%count.*]
                           :from [:auth_user]
                           :where [:= :email (:email data)]})
             first :count zero?)
      (let [password (hashers/encrypt (:password data))
            user-id (->> (db/query<! db {:insert-into :auth_user
                                         :values [(assoc data
                                                    :password password)]})
                         first :id)]
        (when-not (empty? roles)
          (let [role-ids (->> (db/query db {:select [:id]
                                            :from [:auth_role]
                                            :where [:in :name (map name roles)]})
                              (map :id)
                              (into #{}))]
            (if-not (empty? role-ids)
              (db/query! db {:insert-into :auth_user_role
                             :values (map (fn [role-id]
                                            {:role_id role-id
                                             :user_id user-id}) role-ids)}))))
        (when-not (empty? groups)
          (let [group-ids (->> (db/query db {:select [:id]
                                             :from [:auth_group]
                                             :where [:in :name (map name groups)]})
                               (map :id)
                               (into #{}))]
            (if-not (empty? group-ids)
              (db/query! db {:insert-into :auth_user_group
                             :values (map (fn [group-id]
                                            {:group_id group-id
                                             :user_id user-id}) group-ids)}))))
        ;; send back new user
        (auth/get-user db user-id))
      ;; user already exists
      false)))

(extend-type clojure.lang.PersistentArrayMap

  IUserLogin
  (login [{:keys [username password]} db]
    (let [username (str/lower-case username)
          user (-> (db/query db {:select [:id :password]
                                 :from [:auth_user]
                                 :where [:= :username username]})
                   first)]
      (if user
        (if (hashers/check password (:password user))
          (do
            (session/swap! merge {:user-id (:id user)})
            (db/query! db {:update :auth_user
                           :set {:last_login :%now}
                           :where [:= :id (:id user)]})
            true)
          false)
        false)))

  IUserToken
  (enable-token
    ([user db] (reverie.auth/enable-token user 60 db))
    ([user minutes db]
     (let [token (java.util.UUID/randomUUID)]
       (db/query! db {:update :auth_user
                      :set {:token token
                            :token_expire (t/plus (t.local/local-now) (t/minutes minutes))}
                      :where [:= :id (:id user)]})
       token)))
  (retire-token [user db] (db/query! db {:update :auth_user
                                         :set {:token nil}
                                         :where [:= :id (:id user)]}))
  (expired-token? [user db]
    (->> (db/query db {:select [:token]
                       :from [:auth_user]
                       :where [:and
                               [:= :id (:id user)]
                               [:is-not :token nil]
                               [:> :token_expire #sql/raw "now()"]]})
         (empty?))))


(extend-type User

  IUserLogin
  (login [user db]
    (if user
      (do (session/swap! merge {:user-id (:id user)})
          true)
      false))

  IUserUpdate
  (update! [user data db]
    (let [data (reduce (fn [out [k v]]
                         (case k
                           :username (assoc out k v)
                           :email (assoc out k v)
                           :spoken-name (assoc out :spoken_name v)
                           :full-name (assoc out :full_name v)
                           :active? (assoc out :active_p v)))
                       {} data)]
      (db/query! db {:update :auth_user
                     :set data
                     :where [:= :id (:id user)]})))
  (set-password! [user new-password db]
    (db/query! db {:update :auth_user
                   :set {:password (hashers/encrypt new-password)}
                   :where [:= :id (:id user)]}))

  IUserToken
  (enable-token
    ([user db] (reverie.auth/enable-token user 60 db))
    ([user minutes db]
     (let [token (java.util.UUID/randomUUID)]
       (db/query! db {:update :auth_user
                      :set {:token token
                            :token_expire (t/plus (t.local/local-now) (t/minutes minutes))}
                      :where [:= :id (:id user)]})
       token)))
  (retire-token [user db] (db/query! db {:update :auth_user
                                         :set {:token nil}
                                         :where [:= :id (:id user)]}))
  (expired-token? [user db]
    (->> (db/query db {:select [:token]
                       :from [:auth_user]
                       :where [:and
                               [:= :id (:id user)]
                               [:is-not :token nil]
                               [:> :token_expire #sql/raw "now()"]]})
         (empty?))))


(extend-type java.lang.Long

  IUserToken
  (enable-token
    ([id db] (reverie.auth/enable-token id 60 db))
    ([id minutes db]
     (let [token (java.util.UUID/randomUUID)]
       (db/query! db {:update :auth_user
                      :set {:token token
                            :token_expire (t/plus (t.local/local-now) (t/minutes minutes))}
                      :where [:= :id id]})
       token)))
  (retire-token [id db] (db/query! db {:update :auth_user
                                       :set {:token nil}
                                       :where [:= :id id]}))
  (expired-token? [id db]
    (->> (db/query db {:select [:token]
                       :from [:auth_user]
                       :where [:and
                               [:= :id id]
                               [:is-not :token nil]
                               [:> :token_expire #sql/raw "now()"]]})
         (empty?))))


(extend-type java.lang.Integer

  IUserToken
  (enable-token
    ([id db] (reverie.auth/enable-token id 60 db))
    ([id minutes db]
     (let [token (java.util.UUID/randomUUID)]
       (db/query! db {:update :auth_user
                      :set {:token token
                            :token_expire (t/plus (t.local/local-now) (t/minutes minutes))}
                      :where [:= :id id]})

       token)))
  (retire-token [id db] (db/query! db {:update :auth_user
                                       :set {:token nil}
                                       :where [:= :id id]}))
  (expired-token? [id db]
    (->> (db/query db {:select [:token]
                       :from [:auth_user]
                       :where [:and
                               [:= :id id]
                               [:is-not :token nil]
                               [:> :token_expire #sql/raw "now()"]]})
         (empty?))))

(extend-type java.util.UUID

  IUserToken
  (enable-token
    ([id db] (reverie.auth/enable-token id 60 db))
    ([id minutes db] (throw (ex-info "reverie.auth/enable-token can't be used with a UUID"
                                   {:id id
                                    :minutes minutes }))))
  (retire-token [id db] (db/query! db {:update :auth_user
                                       :set {:token nil}
                                       :where [:= :token id]}))
  (expired-token? [id db]
    (->> (db/query db {:select [:token]
                       :from [:auth_user]
                       :where [:and
                               [:= :token id]
                               [:> :token_expire #sql/raw "now()"]]})
         (empty?))))


(extend-type java.lang.String

  IUserToken
  (enable-token
    ([id db] (reverie.auth/enable-token id 60 db))
    ([id minutes db] (throw (ex-info "reverie.auth/enable-token can't be used with a string"
                                   {:id id
                                    :minutes minutes}))))
  (retire-token [id db] (db/query! db {:update :auth_user
                                       :set {:token nil}
                                       :where [:= :token (java.util.UUID/fromString id)]}))
  (expired-token? [id db]
    (try
      (->> (db/query db {:select [:token]
                         :from [:auth_user]
                         :where [:and
                                 [:= :token (java.util.UUID/fromString id)]
                                 [:> :token_expire #sql/raw "now()"]]})
           (empty?))
      (catch Exception _
        true))))
