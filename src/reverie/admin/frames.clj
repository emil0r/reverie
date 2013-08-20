(ns reverie.admin.frames
  (:require [reverie.admin.templates :as t]
            [reverie.auth.user :as user]
            [reverie.core :as rev])
  (:use [hiccup core form]))


(defn- user-info [user]
  (cond
   (and (:first_name user) (:last_name user)) (str (:first_name user) " " (:last_name user))
   (:first_name user) (:first_name user)
   (:last_name user) (:last_name user)
   :else (:name user)))

(rev/defpage "/admin/frame/left" {}
  [:get ["*"] (t/frame-left
               {:css ["/admin/css/font-awesome.min.css"
                      "/admin/css/main.css"]
                :js ["/admin/js/eyespy.js"
                     "/admin/js/init.js"]}
               [:div.user-info "Logged in as " [:span (-> (user/get) user-info)]
                [:div.logout [:a {:href "/admin/logout"}
                              "Logout" [:i.icon-off]]]]
               [:div.tree
                (text-field :tree-search)
                [:i.icon-search]
                [:div#tree]
                [:div.icons
                 [:i.icon-refresh]
                 [:i.icon-plus-sign]
                 [:i.icon-edit-sign]
                 [:i.icon-eye-open.hidden]
                 [:i.icon-trash]]]

               [:div.meta
                "my meta stuff"])])
