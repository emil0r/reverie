(ns reverie.modules.auth
  (:require [buddy.hashers :as hashers]
            [clojure.string :as str]
            [noir.session :as session]
            [reverie.auth :refer [IUserDatabase] :as auth]
            [reverie.core :refer [defmodule]]
            [reverie.database :as db]
            reverie.database.sql
            [reverie.module :as module]
            vlad)
  (:import [reverie.database.sql DatabaseSQL]))

(defmodule auth
  {:name "Authentication"
   :interface? true
   :migration {:path "src/reverie/modules/migrations/auth/"
               :automatic? true}
   :roles #{:admin :staff :user}
   :required-roles #{:admin}
   :template :admin/main
   :entities
   {:user {:name "User"
           :order :id
           :table :auth_user
           :display [:full_name :email]
           :fields {:spoken_name {:name "Spoken name"
                                  :type :text
                                  :max 255}
                    :full_name {:name "Full name"
                                :type :text
                                :max 255}
                    :username {:name "Username"
                               :type :text
                               :validation []
                               :max 255
                               :help "A maximum of 255 characters may be used"}
                    :email {:name "Email"
                            :type :email
                            :validation []
                            :max 255}
                    :password {:name "Password"
                               :type :html
                               :validation []}
                    :active_p {:name "Active?"
                               :type :boolean
                               :default true}
                    :roles {:name "Roles"
                            :type :m2m
                            :table :auth_role
                            :options [:id :name]
                            :order :name
                            :m2m {:table :auth_user_role
                                  :joining [:user_id :role_id]}}
                    :groups {:name "Groups"
                             :type :m2m
                             :table :auth_group
                             :options [:id :name]
                             :order :name
                             :m2m {:table :auth_user_group
                                   :joining [:user_id :group_id]}}}
           :sections [{:fields [:name :password]}
                      {:name "Personal information"
                       :fields [:email :spoken_name :full_name]}
                      {:name "Rights"
                       :fields [:active :is_staff :is_admin :roles]}
                      {:name "Groups"
                       :fields [:groups]}]
           :related {:groups {:relation :many
                              :m2m_table :auth_user_group
                              :table :auth_group}
                     :roles {:relation :many
                             :m2m_table :auth_user_role
                             :table :auth_role}}
           :post nil}
    :group {:name "Group"
            :order :name
            :table :auth_group
            :fields {:name {:name "Name"
                            :type :text
                            :max 255
                            :validation []}
                     :roles {:name "Roles"
                             :type :m2m
                             :table :auth_role
                             :m2m_table :auth_group_role}}
            :sections [{:fields [:name]}
                       {:name "Rights"
                        :fields [:roles]}]}}})


(extend-type DatabaseSQL
  IUserDatabase
  (get-users [db]
    (let [users
          (db/query db {:select [:id :created :username :email
                                 :spoken_name :full_name :last_login]
                        :from [:auth_user]
                        :order-by [:id]})
          roles (group-by
                 :user_id
                 (db/query db {:select [:ur.user_id :r.name]
                               :from [[:auth_user_role :ur]]
                               :join [[:auth_role :r]
                                      [:= :r.id :ur.role_id]]
                               :order-by [:ur.user_id]}))
          groups (group-by
                  :user_id
                  (db/query db {:select [:ug.user_id
                                         [:g.name :group_name]
                                         [:r.name :role_name]]
                                :from [[:auth_user_group :ug]]
                                :join [[:auth_group :g]
                                       [:= :ug.group_id :g.id]]
                                :left-join [[:auth_group_role :gr]
                                            [:= :gr.group_id :ug.group_id]
                                            [:auth_role :r]
                                            [:= :gr.role_id :r.id]]
                                :order-by [:ug.user_id]}))]
      (reduce (fn [out {:keys [id created username
                               email spoken_name full_name last_login]}]
                (conj
                 out
                 (auth/map->User
                  {:id id :created created
                   :username username :email email
                   :spoken-name spoken_name :full-name full_name
                   :last-login last_login
                   :roles (into #{}
                                (remove
                                 nil?
                                 (flatten
                                  [(map #(-> % :name keyword)
                                        (get roles id))
                                   (map #(-> % :role_name keyword)
                                        (get groups id))])))
                   :groups (into #{}
                                 (remove
                                  nil?
                                  (map #(-> % :group_name keyword)
                                       (get groups id))))})))
              [] users)))
  (get-user
    ([db]
       (when-let [user-id (session/get :user-id)]
         (auth/get-user db user-id)))
    ([db id]
       (let [users
             (db/query db (merge
                           {:select [:id :created :username :email
                                     :spoken_name :full_name :last_login]
                            :from [:auth_user]}
                           (cond
                            (and (string? id)
                                 (re-find #"@" id)) {:where [:= :email id]}
                                 (string? id) {:where [:= :username id]}
                                 :else {:where [:= :id id]})))
             id (-> users first :id)
             roles (group-by
                    :user_id
                    (db/query db {:select [:ur.user_id :r.name]
                                  :from [[:auth_user_role :ur]]
                                  :join [[:auth_role :r]
                                         [:= :r.id :ur.role_id]]
                                  :where [:= :ur.user_id id]}))
             groups (group-by
                     :user_id
                     (db/query db {:select [:ug.user_id
                                            [:g.name :group_name]
                                            [:r.name :role_name]]
                                   :from [[:auth_user_group :ug]]
                                   :join [[:auth_group :g]
                                          [:= :ug.group_id :g.id]]
                                   :left-join [[:auth_group_role :gr]
                                               [:= :gr.group_id :ug.group_id]
                                               [:auth_role :r]
                                               [:= :gr.role_id :r.id]]
                                   :where [:= :ug.user_id id]}))]
         (if (first users)
           (let [{:keys [id created username
                         email spoken_name full_name last_login]} (first users)]
             (auth/map->User
              {:id id :created created
               :username username :email email
               :spoken-name spoken_name :full-name full_name
               :last-login last_login
               :roles (into #{}
                            (remove
                             nil?
                             (flatten
                              [(map #(-> % :name keyword)
                                    (get roles id))
                               (map #(-> % :role_name keyword)
                                    (get groups id))])))
               :groups (into #{}
                             (remove
                              nil?
                              (map #(-> % :group_name keyword)
                                   (get groups id))))}))))))
  (login [db username password]
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
        false))))
