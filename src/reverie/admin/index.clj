(ns reverie.admin.index
  (:require [reverie.core :as rev]
            [reverie.admin.templates :as t]))


(rev/defpage "/admin" {}
  [:get ["*"] (t/main {:title "Admin"})])
