(ns reverie.admin.api
  (:require [reverie.core :refer [defpage]]
            [reverie.admin.api.interface.objects :as iobjects]
            [reverie.admin.api.interface.pages :as ipages]))


(defpage "/admin/api" {}
  [["/" {:get ipages/get-pages}]
   ["/interface/pages" {:get ipages/get-pages}]
   ["/interface/pages/:id" {:id #"\d+"} {:id Integer} {:get ipages/get-pages}]
   ["/interface/pages/edit-page" {:post ipages/edit-page!}]
   ["/interface/paged/updated-page" {:post ipages/update-page!}]

   ["/interface/objects/delete" {:post iobjects/delete-object!}]])
