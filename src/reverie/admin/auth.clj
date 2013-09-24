(ns reverie.admin.auth
  (:require [hiccup.form :as form]
            [reverie.admin.templates :as t]
            [reverie.auth.user :as user]
            [reverie.core :as rev]
            [reverie.response :as r]))


(rev/defpage "/admin/login" {}
  [:get ["/"] (t/auth {:title "Admin login"}
                      [:div.row
                       (form/form-to
                        {:class :form-horizontal}
                        [:post ""]
                        [:div.form-group
                         (form/label {:class "control-label col-sm-2"} :username "Username")
                         [:div.col-sm-10
                          (form/text-field {:placeholder "Username" :class :form-control} :username)]]
                        [:div.form-group
                         (form/label {:class "control-label col-sm-2"} :password "Password")
                         [:div.col-sm-10
                          (form/password-field {:class :form-control :placeholder "Password"} :password)]]
                        [:div.form-group
                         [:div.col-sm-offset-2.col-sm-10
                          (form/submit-button {:class "btn btn-primary"} "Log in")]])])]

  [:post ["/" {:keys [username password]}]
   (if (user/login username password)
     (-> "/admin" r/response-302 rev/raise-response)
     (-> "/admin/login" r/response-302 rev/raise-response))])


(rev/defpage "/admin/logout" {}
  [:get ["/"]
   (do
     (user/logout)
     (-> "/admin/login" r/response-302 rev/raise-response))])
