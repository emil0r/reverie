(ns reverie.site.templates.foobar
  (:require [hiccup.page :refer [html5]]
            [reverie.core :refer [deftemplate area]]
            [reverie.page :as page]))

(defn- template-render [request page properties params]
  [:html
   [:head
    [:meta {:charset "UTF-8"}]
    [:title (page/name page)]]
   [:body
    [:div (area a)]
    [:div (area b)]]])


(deftemplate foobar template-render)
(deftemplate foobaz template-render)
