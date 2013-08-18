(ns reverie.admin.auth
  (:require [hiccup.form :as form]
            [reverie.core :as rev]
            [reverie.admin.templates :as t]))


(rev/defpage "/admin/login" {}
  
  [:get ["*"] (t/auth {:title "Admin login"}
                      (form/form-to
                       [:post ""]
                       [:table
                        [:tr
                         [:td (form/label :username "Username")]
                         [:td (form/text-field :username)]]
                        [:tr
                         [:td (form/label :password "Password")]
                         [:td (form/password-field :password)]]
                        [:tr
                         [:td]
                         [:td (form/submit-button "Log in")]]]))]

  [:post ["*" {:keys [username password]}] (t/auth {:title "Admin login"}
                                                   [:table
                                                    [:tr
                                                     [:td "Username"]
                                                     [:td username]]
                                                    [:tr
                                                     [:td "Password"]
                                                     [:td password]]])])
