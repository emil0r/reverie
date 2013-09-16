(ns reverie.admin.modules.auth
  (:require [korma.core :as k]
            [noir.validation :as v]
            [noir.util.crypt :as crypt])
  (:use [hiccup form]
        [reverie.core :only [defmodule raise-response]]
        [reverie.admin.frames.common :only [error-item frame-options]]
        reverie.admin.modules.helpers
        [reverie.admin.templates :only [frame]]
        [reverie.responses :only [response-302]]
        [reverie.util :only [join-uri]]))

(def required-field "This field is required")

(defn- get-password-form []
  (form-to
   {:id :password-form}
   [:post ""]
   [:fieldset
    [:legend "Change password"]
    [:div.form-row
     (v/on-error :password error-item)
     (label :password "Password")
     (password-field {:min 8} :password)]
    [:div.form-row
     (v/on-error :repeat-password error-item)
     (label :repeat-password "Repeat password")
     (password-field {:min 8} :repeat-password)
     (form-help-text {:help "Make sure the password is the same"})]]
   [:div.buttons
    [:input {:type :submit :class "btn btn-primary"
             :id :_cancel :name :_cancel :value "Cancel"}]
    [:input {:type :submit :class "btn btn-primary"
             :id :_change :name :_change :value "Change password"}]]))

(defn- valid-form-data? [form-data]
  (do
    (v/rule (v/has-value? (form-data :password)) [:password required-field])
    (v/rule (v/has-value? (form-data :repeat-password)) [:repeat-password required-field])
    (v/rule (v/min-length? (form-data :password) 8) [:password "Minimum 8 characters in the password"])
    (v/rule (= (form-data :password) (form-data :repeat-password)) [:password "The passwords are not matching. Try again."]))
  (not (v/errors? :password :repeat-password)))

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
                                     :validation [[v/has-value? required-field]]
                                     :max 255
                                     :help "A maximum of 255 characters may be used"}
                              :email {:name "Email"
                                      :type :email
                                      :validation [[v/is-email? required-field]]
                                      :max 255}
                              :password {:name "Password"
                                         :type :html
                                         :validation [[(fn [{:keys [password repeat-password]}]
                                                         (= password repeat-password))
                                                       "The passwords don't match. Please type them in again." :use-form-data]
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
                      :order :name
                      :fields {:name {:name "Name"
                                      :type :text
                                      :max 255
                                      :validation [[v/has-value? required-field]]}
                               :roles {:name "Roles"
                                       :type :m2m
                                       :table :role
                                       :options [:id :name]
                                       :connecting-table :role_group}}
                      :sections [{:fields [:name]}
                                 {:name "Rights"
                                  :fields [:roles]}]}}
   }
  [:get ["/:entity/:id/password"]
   (frame
    frame-options
    (navbar request)
    (get-password-form))]
  [:post ["/:entity/:id/password" form-data]
   (let [{:keys [module module-name]} (get-module request)]
    (if (valid-form-data? form-data)
      (do
        (k/update (get-entity-table entity module)
                  (k/set-fields {:password (crypt/encrypt (:password form-data))})
                  (k/where {:id (read-string id)}))
        (raise-response (response-302 (frame-join-uri module-name entity))))
      (frame
       frame-options
       (navbar request)
       (get-password-form))))])

