(ns reverie.admin.auth
  (:require [hiccup.form :as form]
            [reverie.admin.templates :as t]
            [reverie.auth.user :as user]
            [reverie.core :as rev]
            [reverie.responses :as r]
            ))


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

  [:post ["*" {:keys [username password]}]
   (if (user/login username password)
     (-> "/admin" r/response-302 rev/raise-response)
     (-> "/admin/login" r/response-302 rev/raise-response))])
