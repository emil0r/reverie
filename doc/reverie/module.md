# module

Technical documentation for module.


## example implementaion

This example implementaion is based on the auth module in reverie.modules.auth.

```clojure

(ns some-namespace
  (:require [reverie.core :refer [defmodule]]))
  

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

           ;; order by
           :order :id
           
           ;; which table to use
           :table :auth_user
           
           ;; what to display when showing the data in the entity section in the
           ;; automatic admin interface
           :display [:username :email]
           
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
            :order :name
            :table :auth_group
            :display [:name]
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

-  No way of filter results in the entity view.
-  No way of dealing with bulk.
-  No way of sorting other than the default sort.
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
              :order :id
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
