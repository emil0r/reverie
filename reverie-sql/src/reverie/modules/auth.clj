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
   :entities
   {:user {:name "User"
           :order :name
           :display [:name :email]
           :fields {:first_name {:name "First name"
                                 :type :text
                                 :max 255}
                    :last_name {:name "Last name"
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
                               :default true}}
           :sections [{:fields [:name :password]}
                      {:name "Personal information"
                       :fields [:email :first_name :last_name]}
                      {:name "Rights"
                       :fields [:active :is_staff :is_admin :roles]}
                      {:name "Groups"
                       :fields [:groups]}]
           :related {:groups {:relation :many
                              :table :auth_user_group}
                     :roles {:relation :many
                             :table :auth_user_role}}
           :post nil}
    :group {:name "Group"
            :order :name
            :fields {:name {:name "Name"
                            :type :text
                            :max 255
                            :validation []}}
            :related {:roles {:relation :many
                              :table :auth_group_role}}
            :sections [{:fields [:name]}
                       {:name "Rights"
                        :fields [:roles]}]
            }}}
  [])


(extend-type DatabaseSQL
  IUserDatabase
  (get-users [db]
    (let [users
          (db/query db {:select [:id :created :username :email
                                 :first_name :last_name :last_login]
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
                               email first_name last_name last_login]}]
                (conj
                 out
                 (auth/map->User
                  {:id id :created created
                   :username username :email email
                   :first-name first_name :last-name last_name
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
  (get-user [db id]
    (let [users
          (db/query db (merge
                        {:select [:id :created :username :email
                                  :first_name :last_name :last_login]
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
                      email first_name last_name last_login]} (first users)]
          (auth/map->User
           {:id id :created created
            :username username :email email
            :first-name first_name :last-name last_name
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
                                (get groups id))))})))))
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
            true)
          false)
        false))))
