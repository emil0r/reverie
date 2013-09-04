(ns reveriecms.templates.main
  (:require [reverie.core :as rev])
  (:use [hiccup core page]))


(rev/deftemplate main {}
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:title (str (-> request :page :title) " &mdash; reverie/cms")]
    [:style {:type "text/css"} ".reverie-area {border: 1px black solid;}"]]
   [:body
    "uri -> " (:uri request)
    "<br/>"
    "mode -> " (:mode request)
    (rev/area :a)]))
