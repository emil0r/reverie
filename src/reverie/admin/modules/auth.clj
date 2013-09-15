(ns reverie.admin.modules.auth
  (:require [noir.validation :as v]
            [noir.util.crypt :as crypt])
  (:use [hiccup form]
        [reverie.core :only [defmodule]]
        [reverie.admin.frames.common :only [error-item]]
        [reverie.admin.modules.helpers :only [form-help-text]]
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
                                     :help "A maximum of 255 characters may be used"}
                              :email {:name "Email"
                                      :type :email
                                      :max 255}
                              :password {:name "Password"
                                         :type :html
                                         :validation [[(fn [{:keys [password repeat-password]}]
                                                         (= password repeat-password))
                                                       "The passwords don't match. Please type them in again."]
                                                      [v/has-value? "A password is mandatory"]]
                                         :html (fn [[field data] {:keys [form-data real-uri entity-id]}]
                                                 (if entity-id
                                                   [:div.form-row
                                                    [:label "Password"]
                                                    [:a {:href (join-uri real-uri "/password")} "Change password"]]
                                                   (list
                                                    [:div.form-row
                                                     (v/on-error field error-item)
                                                     (label field "Password")
                                                     (password-field field (form-data field))
                                                     (form-help-text {})]
                                                    [:div.form-row
                                                     (v/on-error :repeat-password error-item)
                                                     (label :repeat-password "Repeat password")
                                                     (password-field :repeat-password (form-data :repeat-password))
                                                     (form-help-text {:help "Make sure the password is the same"})])))}
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
                     :post (fn [data mode]
                             (if (= mode :edit)
                               (dissoc data :password)
                               (dissoc data :repeat-password)))}
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

