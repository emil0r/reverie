(ns reverie.admin.template
  (:require [hiccup.page :refer [html5]]
            [hiccup.form :as form]
            [reverie.core :refer [deftemplate area]]))


(defn admin-login [request page properties {:keys [username password]}]
  (html5
   [:head
    [:meta {:charset "UTF-8"}]
    [:title "Reverie Admin Login"]]
   [:body
    [:div.row
     [:div.col-md-4.col-md-offset-4
      [:form {:method "POST"}
       [:table.table
        [:tr
         [:th (form/label :username "Username")]
         [:td (form/text-field :username username)]]
        [:tr
         [:th (form/label :password "Password")]
         [:td (form/password-field :password)]]
        [:tr
         [:th]
         [:td (form/submit-button "Log in")]]]]]]]))

(defn- admin-main [request page properties params]
  (html5
   [:head
    [:meta {:charset "UTF-8"}]
    [:title "Reverie"]]
   [:body
    [:div.row
     [:div.col-md-12
      [:nav (area nav)]]]
    [:div.row
     [:div.col-md-6 (area a)]
     [:div.col-md-6 (area b)]]]))

(deftemplate admin/main admin-main)
(deftemplate admin/login admin-login)
