(ns reverie.auth.sql
  (:require [buddy.hashers :as hashers]
            [reverie.auth :as auth]
            [reverie.database :as db]
            [reverie.module :as m]
            [reverie.page :as page]
            [reverie.auth :refer [IAuthorize IUserAdd]]
            [reverie.util :as util]
            [taoensso.timbre :as log])
  (:import [reverie.page Page AppPage RawPage]
           [reverie.module Module]))

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
