(ns reverie.admin.index
  (:require [reverie.core :as rev]
            [reverie.admin.templates :as t])
  (:use [hiccup core page]))


(rev/defpage "/admin" {}
  [:get ["*"] (t/main {:title "Admin"})])

(rev/defpage "/admin/login" {}
  [:get ["*"] (t/auth {:title "Admin login"})])
