(ns reverie.modules.auth
  (:require [buddy.hashers :as hashers]
            [clojure.string :as str]
            [ez-web.breadcrumbs :refer [crumb]]
            [ez-web.uri :refer [join-uri]]
            [hiccup.form :as form]
            [noir.session :as session]
            [reverie.admin.looknfeel.form :as looknfeel]
            [reverie.auth :refer [IUserDatabase
                                  IUserLogin] :as auth]
            [reverie.core :refer [defmodule]]
            [reverie.database :as db]
            reverie.database.sql
            [reverie.module :as m]
            [reverie.module.entity :as e]
            [reverie.modules.default :refer [base-link pk-cast
                                             get-display-name
                                             process-request
                                             select-errors]]
            [reverie.modules.sql :as msql]
            [ring.util.response :as response]
            vlad)
  (:import [reverie.database.sql DatabaseSQL]))


(defn- repeat-password-field [form-params errors]
  [:div.form-row
   (looknfeel/error-items :repeat-password errors {[:repeat-password] "Repeat password"})
   (form/label :repeat-password "Repeat password")
   (form/password-field :repeat-password (form-params :repeat-password))
   (looknfeel/help-text {:help "Make sure the password is the same"})])

(defn- change-password [request module {:keys [entity id] :as params}
                        & [errors]]
  (let [{:keys [entity
                id
                form-params]} (process-request request module true)
        entity-data (m/get-data module entity params id)]
    {:nav (:crumbs (crumb [[(join-uri base-link (m/slug module)) (m/name module)]
                           [(e/slug entity) (e/name entity)]
                           [(str id) (get-display-name entity entity-data)]
                           ["password" "Change password"]]))
     :content (form/form-to {:id :password-form}
                            ["POST" ""]
                            [:fieldset
                             [:legend "Change password"]
                             [:div.form-row
                              (looknfeel/error-items :password errors (e/error-field-names entity))
                              (form/label :password "Password")
                              (form/password-field {:min 8} :password (form-params :password))]
                             (repeat-password-field form-params errors)]
                            [:div.buttons
                             [:input.btn.btn-primary {:type :submit :id :_cancel :name :_cancel :value "Cancel"}]
                             [:input.btn.btn-primary {:type :submit :id :_change :name :_change :value "Change password"}]])}))

(defn- handle-change-password [request module params]
  (let [{:keys [entity
                id
                pre-save-fn
                form-params
                errors]} (process-request request module true)
        errors (select-errors errors [[:password] [:repeat-password]])]
    (if (empty? errors)
      (do
        (db/query! (:database module)
                   {:update (e/table entity)
                    :set {:password (hashers/encrypt (:password form-params))}
                    :where [:= (e/pk entity) id]})
        (response/redirect (join-uri base-link
                                     (m/slug module)
                                     (e/slug entity)
                                     (str id))))
      (change-password request module params errors))))

(defn- password-html [entity field {:keys [form-params entity-id uri
                                           errors error-field-names]}]
  (if entity-id
    [:div.form-row
     [:label "Password"]
     [:a {:href (join-uri uri "/password")} "Change password"]]
    (list
     [:div.form-row
      (looknfeel/error-items field errors error-field-names)
      (form/label field (e/field-name entity field))
      (form/password-field (e/field-attribs entity field) field (form-params field))
      (looknfeel/help-text (e/field-options entity field))]
     (repeat-password-field form-params errors))))

(defn- user-post-fn [data edit?]
  (if edit?
    (dissoc data :password)
    (dissoc data :repeat-password)))

(defn- user-pre-save-fn [data edit?]
  (if edit?
    data
    (assoc data :password (hashers/encrypt (:password data)))))

(defmodule auth
  {:name "Authentication"
   :interface? true
   :migration {:path "src/reverie/modules/migrations/auth/"
               :automatic? true}
   ;; TODO
   ;; roles and actions need to be controlled on a high
   ;; level kind of view and a low level view where it
   ;; is on an individual basis
   ;; the high level view can be controlled by the module/page/whatever
   ;; by itself, but the low level view will require an interface
   ;; that should be under the auth module
   :roles #{:admin :staff :user :all}
   :actions #{:view :edit}
   :required-roles {:view #{:admin :staff}
                    :edit #{:admin :staff}}
   :template :admin/main
   :entities
   {:user {:name "User"
           :order :id
           :table :auth_user
           :display [:username :email]
           :fields {:spoken_name {:name "Spoken name"
                                  :type :text
                                  :max 255
                                  :validation (vlad/present [:spoken_name])}
                    :full_name {:name "Full name"
                                :type :text
                                :max 255
                                :validation (vlad/present [:full_name])}
                    :username {:name "Username"
                               :type :text
                               :validation (vlad/present [:username])
                               :max 255
                               :help "A maximum of 255 characters may be used"}
                    :email {:name "Email"
                            :type :email
                            :validation (vlad/present [:email])
                            :max 255}
                    :password {:name "Password"
                               :type :html
                               :html password-html
                               :validation (vlad/chain
                                            (vlad/join
                                             (vlad/present [:password])
                                             (vlad/length-in 8 128 [:password])
                                             (vlad/present [:repeat-password]))
                                            (vlad/equals-field [:password] [:repeat-password]))}
                    :active_p {:name "Active?"
                               :type :boolean
                               :default true}
                    :roles {:name "Roles"
                            :type :m2m
                            :cast :int
                            :table :auth_role
                            :options [:id :name]
                            :order :name
                            :m2m {:table :auth_user_role
                                  :joining [:user_id :role_id]}}
                    :groups {:name "Groups"
                             :type :m2m
                             :cast :int
                             :table :auth_group
                             :options [:id :name]
                             :order :name
                             :m2m {:table :auth_user_group
                                   ;; joining: this that
                                   :joining [:user_id :group_id]}}}
           :sections [{:fields [:username :password]}
                      {:name "Personal information"
                       :fields [:email :spoken_name :full_name]}
                      {:name "Rights"
                       :fields [:active_p :roles]}
                      {:name "Groups"
                       :fields [:groups]}]
           :post user-post-fn
           :pre-save user-pre-save-fn}
    :group {:name "Group"
            :order :name
            :table :auth_group
            :display [:name]
            :fields {:name {:name "Name"
                            :type :text
                            :max 255
                            :validation (vlad/present [:name])}
                     :roles {:name "Roles"
                             :type :m2m
                             :cast :int
                             :table :auth_role
                             :options [:id :name]
                             :order :name
                             :m2m {:table :auth_group_role
                                   :joining [:group_id :role_id]}}}
            :sections [{:fields [:name]}
                       {:name "Rights"
                        :fields [:roles]}]}}}
  [["/:entity/:id/password" {:get change-password
                             :post handle-change-password}]])


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
                                   (get groups id))))})))))))

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
        false))))
