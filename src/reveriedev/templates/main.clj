(ns reveriedev.templates.main
  (:require [reverie.core :as rev])
  (:use [hiccup core page]))


(rev/deftemplate main {}
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:title (str (-> request :page :title) " &mdash; reverie/dev")]
    (map include-css ["/admin/css/editing.css"])
    (map include-js ["/admin/js/eyespy.js"
                     "/admin/js/init.js"])]
   [:body
    [:div {:style "margin-bottom: 100px;"}
     "uri -> " (:uri request)
     "<br/>"
     "mode -> " (:mode request)]
    [:div {:style "float: left; width: 400px;"}
     (rev/area :a)]
    [:div {:style "float: left; width: 400px; margin-left: 30px;"}
     (rev/area :b)]
    [:div {:style "clear: both;"}]]))
