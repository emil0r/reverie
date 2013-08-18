(ns reverie.admin.auth
  (:require [reverie.core :as rev]
            [reverie.admin.templates :as t]))


(rev/defpage "/admin/login" {}
  [:get ["*"] (t/auth {:title "Admin login"}
                      "asdf")])
