(ns reverie.admin.api
  (:require [reverie.core :refer [defpage]]
            [reverie.admin.api.interface.pages :as ipages]))


(defpage "/admin/api" {}
  [["/" {:get ipages/get-pages}]
   ["/interface/pages" {:get ipages/get-pages}]
   ["/interface/pages/:id" {:id #"\d+"} {:id Integer} {:get ipages/get-pages}]])
