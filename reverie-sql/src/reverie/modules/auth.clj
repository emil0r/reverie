(ns reverie.modules.auth
  (:require [buddy.hashers :as hashers]
            [reverie.core :refer [defmodule]]
            [reverie.module :as module]
            vlad))

(defmodule auth
  {:name "Authentication"
   :interface? true
   :migration {:path "src/reverie/modules/migrations/auth/"
               :automatic? true}
   :roles #{:admin :staff :user}
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
