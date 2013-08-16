(ns reveriecms.templates.main
  (:require [reverie.core :as rev])
  (:use [hiccup core page]))


(rev/deftemplate main {}
  (html5
   [:head
    [:charset "utf-8"]
    [:title (str (-> request :page :title) " &mdash; reverie/cms")]]
   [:body
    (rev/area :a)]))
