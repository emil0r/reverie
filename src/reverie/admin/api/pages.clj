(ns reverie.admin.api.pages
  (:require [reverie.core :as rev])
  (:use [ring.middleware.edn :only [wrap-edn-params]]))


(rev/defpage "/admin/api/pages" {}
  [:get ["/read" {:middleware [[wrap-edn-params]]}
         ]
   (pr-str (:edn-params request))
   ])
