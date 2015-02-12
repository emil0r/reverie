(ns reverie.admin.templates
  (:require [hiccup.page :refer [html5]]
            [hiccup.form :as form]
            [reverie.core :refer [deftemplate area]]))

(defn- head [title]
  [:head
   [:link {:rel "stylesheet" :type "text/css" :href "/admin/css/main.css"}]
   [:meta {:charset "UTF-8"}]
   [:title title]])

(defn- footer []
  )

(defn admin-index [request page properties params]
  (html5
   (head "Admin reverie")
   [:body
    [:table.controller
     {:cellpadding 0 :cellspacing 0
      :style "width: 100%; height: 100%;"}
     [:tr
      [:td#controlpanel {:style "width: 240px;"}
       [:iframe {:src "/admin/frame/controlpanel" :frameborder "no" :id "framecontrol" :name "framecontrol"}]]
      [:td#main
       [:iframe {:src "/" :frameborder "no" :id "framemain" :name "framemain"}]]
      [:td#options
       [:iframe {:src "/admin/frame/options" :frameborder "no" :id "frameoptions" :name "frameoptions"}]]]]]))

(defn admin-login [request page properties {:keys [username password]}]
  (html5
   (head "reverie Admin Login")
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

(defn- admin-controlpanel [request page properties params]
  (html5
   (head "reverie Control Panel")
   [:body
    "control panel"]))

(defn- admin-main [request page properties params]
  (html5
   (head "reverie")
   [:body
    [:div.row
     [:div.col-md-12
      [:nav (area nav)]]]
    [:div.row
     [:div.col-md-6 (area a)]
     [:div.col-md-6 (area b)]]]))

(deftemplate admin/main admin-main)
(deftemplate admin/login admin-login)
(deftemplate admin/index admin-index)
(deftemplate admin/control-panel admin-controlpanel)
