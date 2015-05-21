(ns reverie.admin.templates
  (:require [hiccup.page :refer [html5]]
            [hiccup.form :as form]
            [reverie.admin.looknfeel.common :refer [head footer]]
            [reverie.core :refer [deftemplate area]]
            [ring.util.anti-forgery :refer :all]))

(defn admin-index [request page properties params]
  (html5
   (head "Admin reverie" {:request request})
   [:body
    [:div#container
     [:iframe {:src "/admin/frame/controlpanel" :frameborder "no" :id "framecontrol" :name "framecontrol"}]
     [:iframe {:src "/" :frameborder "no" :id "framemain" :name "framemain"}]
     [:iframe {:src "/admin/frame/options" :frameborder "no" :id "frameoptions" :name "frameoptions"}]]]))

(defn admin-login [request page properties {:keys [username password]}]
  (html5
   (head "reverie Admin Login")
   [:body.admin-login
    [:div.row
     [:div.col-md-4.col-md-offset-4
      [:img.img-responsive {:src "/static/admin/img/reveriecms.png" :alt "reverie/CMS"}]]]
    [:div.row
     [:div.col-md-4.col-md-offset-4
      [:form {:method "POST"}
       (anti-forgery-field)
       [:table.table
        [:tr
         [:th (form/label :username "Username")]
         [:td (form/text-field :username username)]]
        [:tr
         [:th (form/label :password "Password")]
         [:td (form/password-field :password)]]
        [:tr
         [:th]
         [:td (form/submit-button {:class "btn btn-primary"} "Log in")]]]]]]]))

(defn- admin-controlpanel [request page properties params]
  (html5
   (head "reverie Control Panel")
   [:body.controlpanel
    (area panel)
    (footer {:request request})]))

(defn- admin-main [request page properties params]
  (html5
   (head "reverie")
   [:body
    [:nav
     [:div.container (area nav)]]

    [:div.container
     [:div.row.admin-interface
      [:div.content
       (area content)]
      [:div.pagination
       (area pagination)]
      [:footer
       (area footer)]]]]))

(deftemplate admin/main admin-main)
(deftemplate admin/login admin-login)
(deftemplate admin/index admin-index)
(deftemplate admin/control-panel admin-controlpanel)
