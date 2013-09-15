(ns reverie.admin.modules.auth
  (:require [noir.util.crypt :as crypt])
  (:use [reverie.core :only [defmodule]]
        [reverie.util :only [join-uri]]))


(defmodule auth
  {:name "Authentication"
   :admin? true
   :entities {:user {:name "User"
                     :order :name
                     :display [:name :email]
                     ;; :display [[:o2m-table :field-to-show] fn-to-run]
                     :fields {:first_name {:name "First name"
                                           :type :text
                                           :max 255}
                              :last_name {:name "Last name"
                                          :type :text
                                          :max 255}
                              :name {:name "Username"
                                     :type :text
                                     :max 255
                                     :validation [[(fn [_] false) "my error message"]]
                                     
                                     :help "A maximum of 255 characters may be used"}
                              :email {:name "Email"
                                      :type :email
                                      :max 255}
                              :password {:name "Password"
                                         :type :html
                                         :html (fn [[field _] {:keys [real-uri]}]
                                                 [:div.form-row
                                                  [:label "Password"]
                                                  [:a {:href (join-uri real-uri "/password")} "Change password"]])}
                              :active {:name "Active?"
                                       :type :boolean
                                       :default true}
                              :is_staff {:name "Staff?"
                                         :type :boolean
                                         :default false}
                              :is_admin {:name "Administrator?"
                                         :type :boolean
                                         :default false}
                              :groups {:name "Groups"
                                       :type :m2m
                                       :table :group
                                       :options [:id :name]}
                              :roles {:name "Roles"
                                      :type :m2m
                                      :table :role
                                      :connecting-table :role_user
                                      :options [:id :name]}}
                     :sections [{:fields [:name :password]}
                                {:name "Personal information"
                                 :fields [:first_name :last_name :email]}
                                {:name "Rights"
                                 :fields [:active :is_staff :is_admin :roles]}
                                {:name "Groups"
                                 :fields [:groups]}]
                     :post (fn [data]
                             (if (empty? (:password data))
                               data
                               (assoc data :password
                                      (crypt/encrypt (:password data)))))}
              :group {:name "Group"
                      :fields {:name {:name "Name"
                                      :type :text
                                      :max 255}
                               :roles {:name "Roles"
                                       :type :m2m
                                       :table :role
                                       :connecting-table :role_group}}
                      :sections [{:fields [:name]}
                                 {:name "Rights"
                                  :fields [:roles]}]}}
   }
  [:get ["/:entity/:id/password"]
   "mah password"])

