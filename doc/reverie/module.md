# module

Technical documentation for module.


## example implementaion

This example implementaion is based on the auth module in reverie.modules.auth.

```clojure

(ns reverie.modules.auth
  (:require [reverie.core :refer [defmodule]]
            ;; as well as other requires
            ))


;; below here are supporting functions. jump to defmodule for the
;; definition of the module

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
                form-params]} (process-request request module true :change-password)
        entity-data (m/get-data module entity params id)]
    {:nav (:crumbs (crumb [[(join-uri base-link (m/slug module)) (m/name module)]
                           [(e/slug entity) (e/name entity)]
                           [(str id) (get-display-name entity entity-data)]
                           ["password" "Change password"]]))
     :content (form/form-to {:id :password-form}
                            ["POST" ""]
                            (anti-forgery-field)
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
  (let [{:keys [entity id pre-save-fn form-params errors]} (process-request request module true :change-password)
        errors (select-errors errors [:password :repeat-password])
        redirect-url (join-uri base-link (m/slug module) (e/slug entity) (str id))]
    (if (contains? params :_cancel)
      (response/redirect redirect-url)
      (if (empty? errors)
        (do
          (db/query! (:database module)
                     {:update (e/table entity)
                      :set {:password (hashers/encrypt (:password form-params))}
                      :where [:= (e/pk entity) id]})
          (response/redirect redirect-url))
        (change-password request module params errors)))))

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



(defn search-query-user [{:keys [database database-name limit offset interface active_p username email] :as params}]
  (let [[role group] (->> [:role :group]
                          (map params)
                          (map edn/read-string))
        order (get-order-query interface params)]
    (->> (-> {:select [:u.id :u.username :u.email :u.active_p]
              :modifers [:distinct]
              :from [[:auth_user :u]]
              :order-by [order]
              :limit limit
              :offset offset}
             (swap (or role group)
                   (sql.helpers/join (optional role [:auth_user_role :ur] [:= :ur.user_id :u.id])
                                     (optional role [:auth_role :r] [:= :ur.role_id :r.id])
                                     (optional group [:auth_user_group :ug] [:= :ug.user_id :u.id])
                                     (optional group [:auth_group :g] [:= :ug.group_id :g.id])))
             (swap (or role
                       group
                       (not (str/blank? active_p))
                       (not (str/blank? username))
                       (not (str/blank? email)))
                   (sql.helpers/where [:and
                                       (optional role [:= :r.id role])
                                       (optional group [:= :g.id group])
                                       (optional (not (str/blank? username)) [:ilike :u.username username])
                                       (optional (not (str/blank? email)) [:ilike :u.email email])
                                       (optional (= "True" active_p) [:= :u.active_p true])
                                       (optional (= "False" active_p) [:= :u.active_p false])]))
             (clean))
         (db/query database database-name))))

(def filter-user [{:name :username
                   :type :text}
                  {:name :email
                   :type :text}
                  {:name :role
                   :type :dropdown
                   :options (fn [{:keys [database database-name]}]
                              (->> {:select [:id :name]
                                    :from [:auth_role]
                                    :order-by [:name]}
                                   (db/query database database-name)
                                   (map (juxt #(-> % :id str) :name))
                                   (into [["" ""]])))}
                  {:name :group
                   :type :dropdown
                   :options (fn [{:keys [database database-name]}]
                              (->> {:select [:id :name]
                                    :from [:auth_group]
                                    :order-by [:name]}
                                   (db/query database database-name)
                                   (map (juxt #(-> % :id str) :name))
                                   (into [["" ""]])))}
                  {:name :active_p
                   :label "Active?"
                   :type :dropdown
                   :options ["" "True" "False"]}])


(defn search-query-group [{:keys [database database-name limit offset interface active_p] :as params}]
  (let [[role] (->> [:role]
                    (map params)
                    (map edn/read-string))
        order (get-order-query interface params)]
    (->> (-> {:select [:g.id :g.name]
              :modifers [:distinct]
              :from [[:auth_group :g]]
              :order-by [order]
              :limit limit
              :offset offset}
             (swap role
                   (sql.helpers/join (optional role [:auth_group_role :gr] [:= :gr.group_id :g.id])
                                     (optional role [:auth_role :r] [:= :gr.role_id :r.id])))
             (swap role
                   (sql.helpers/where [:and
                                       (optional role [:= :r.id role])]))
             (clean))
         (db/query database database-name))))

(def filter-group [{:name :role
                    :type :dropdown
                    :options (fn [{:keys [database database-name]}]
                               (->> {:select [:id :name]
                                     :from [:auth_role]
                                     :order-by [:name]}
                                    (db/query database database-name)
                                    (map (juxt #(-> % :id str) :name))
                                    (into [["" ""]])))}])

  


(defmodule auth
  {
   ;; what will show up in the interface
   :name "Authentication"
   
   ;; automatic interface. will take :entities and create an interface
   ;; from this
   :interface? true
   
   ;; any migrations needed to be done
   :migration {:path "resources/migrations/modules/auth/"
               :automatic? true}

   ;; roles supported by the module
   :roles #{:admin :staff :user :all}
   
   ;; actions supported by the module
   :actions #{:view :edit}
   
   ;; which roles are required for which actions
   :required-roles {:view #{:admin :staff}
                    :edit #{:admin :staff}}
                    
   ;; template to use. :admin/main is recommended, but can be overridden
   :template :admin/main
   
   ;; entities supported by the module
   :entities
   {:user {;; name of the entity
           :name "User"
           
           ;; which table to use
           :table :auth_user
                      
           ;; BREAKING CHANGE
           ;; all of interface options for defmodule has been moved
           ;; into the :interface key
           :interface {;; how to handle display
                       :display {;; the key links to the column name
                                 :username {;; what to display
                                            :name "Username"
                                            ;; does this link to the admin interface
                                            ;; for manipulating the entry?
                                            :link? true
                                            ;; what is the sort variable called?
                                            :sort :u
                                            ;; what do we sort on if not the key?
                                            :sort-name :id}
                                 :email {:name "Email"
                                         :sort :e}
                                 :active_p {:name "Active?"
                                            :sort :a}}
                       ;; default order
                       :default-order :id
                       ;; the function for retuning the query to use
                       :query search-query-user
                       ;; the filter to use
                       :filter filter-user}
           
           ;; which fields are supported by the entity
           :fields {:spoken_name {:name "Spoken name"
                                  :type :text
                                  :max 255
                                  :validation (vlad/attr [:spoken_name] (vlad/present))}
                    :full_name {:name "Full name"
                                :type :text
                                :max 255
                                :validation (vlad/attr [:full_name] (vlad/present))}
                    :username {:name "Username"
                               :type :text
                               :validation (vlad/attr [:username] (vlad/present))
                               :max 255
                               :help "A maximum of 255 characters may be used"}
                    :email {:name "Email"
                            :type :email
                            :validation (vlad/attr [:email] (vlad/present))
                            :max 255}
                    :password {:name "Password"
                               :type :html
                               :html password-html
                               :validation-skip-stages [:edit]
                               :validation (vlad/chain
                                            (vlad/join
                                             (vlad/attr [:password] (vlad/present))
                                             (vlad/attr [:password] (vlad/length-in 8 128 ))
                                             (vlad/attr [:repeat-password] (vlad/present)))
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
                                   
           ;; sections for the different fields
           :sections [{:fields [:username :password]}
                      {:name "Personal information"
                       :fields [:email :spoken_name :full_name]}
                      {:name "Rights"
                       :fields [:active_p :roles]}
                      {:name "Groups"
                       :fields [:groups]}]
                       
           ;; post function. this function runs after a post has been done
           :post user-post-fn
           
           ;; pre save function. runs just before an entity is being saved
           :pre-save user-pre-save-fn}
           
    ;; second entity -> Group
    :group {:name "Group"
            :table :auth_group
            :interface {:display {:name {:name "Name"
                                         :link? true
                                         :sort :n}}
                        :default-order :name
                        :query search-query-group
                        :filter filter-group}
            :fields {:name {:name "Name"
                            :type :text
                            :max 255
                            :validation (vlad/attr [:name] (vlad/present))}
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
                        
  ;; you can add optional routes
  ;; the automatic interface specifies a number of routes already,
  ;; which is followed by the the optional routes.
  ;; use this option when you want to add extra functionality
  
  ;; in the event that the automatic admin interface is not used
  ;; this is where you will have to handle the routing
  [["/:entity/:id/password" {:get change-password
                             :post handle-change-password}]])
```

## authorization

While supported by modules to a certain extent, authorization is not fully developed yet, so use it with caution.


## automatic admin interface

The automatic admin interface works reasonably well, but suffers from the following:

-  No way of dealing with bulk.
-  No inline display.
-  M2M field is clunky as it requires a javascript widget which is not yet written.


## publishing

Modules support publishing where you can have version control built into the module.


```clojure

(defn published-fn? [module entity id]
  (->> {:select [:%count.*]
        :from [:blog_post]
        :where [:= :id id]}
       (db/query (:database module))
       first :count zero? false?))

(defn publish-fn [module entity id]
  ;; add to history tables from published table
  ;; insert/update published table from draft table
  )

(defn unpublish-fn [module entity id]
  ;; add to history tables from published table
  ;; delete from published table
  )

(defn delete-fn [module entity id]
  ;; delete from history tables
  ;; delete from published table
  ;; delete from draft table
  )

(defmodule blog
  {
   :entities {:name "Blog post"
              :interface {...}
              :table :blog_draft
              :publishing {;; show un/publish button(s)?
                           :publish? true
                           
                           ;; run this to publish
                           :publish-fn publish-fn
                           
                           ;; run this to unpublish
                           :unpublish-fn unpublish-fn
                           
                           ;; how do we delete?
                           ;; this functions needs to deal with not
                           ;; only deleting data from the table blog_draft
                           ;; but also from any other tables that holds
                           ;; the versioned data
                           :delete-fn delete-fn
                           
                           ;; is anything published?
                           :published?-fn published?-fn}
                           }
  })
```
