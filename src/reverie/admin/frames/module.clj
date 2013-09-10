(ns reverie.admin.frames.module
  (:require [reverie.admin.templates :as t]
            [reverie.atoms :as atoms]
            [reverie.core :as rev])
  (:use [hiccup core]
        [reverie.admin.frames.common :only [frame-options]]
        [reverie.middleware :only [wrap-access]]))


(rev/defpage "/admin/frame/module" {:middleware [[wrap-access :edit]]}
  [:get ["/"]
   (t/frame
    (dissoc frame-options :js)
    [:h1 "You have to pick a module!"])])
