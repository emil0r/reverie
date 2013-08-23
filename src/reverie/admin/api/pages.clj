(ns reverie.admin.api.pages
  (:require [reverie.core :as rev])
  (:use [reverie.middleware :only [wrap-edn-response]]
        [ring.middleware.edn :only [wrap-edn-params]]))


(rev/defpage "/admin/api/pages" {:middleware [[wrap-edn-params]
                                              [wrap-edn-response]]}
  [:get ["/read" ]
   (pr-str
    [1 2 3 4])
   ])
