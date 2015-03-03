(ns reverie.admin.api
  (:require [reverie.core :refer [defpage]]
            [reverie.admin.api.interface.frames :as iframes]
            [reverie.admin.api.interface.objects :as iobjects]
            [reverie.admin.api.interface.pages :as ipages]))


(defpage "/admin/api" {}
  [["/" {:get ipages/get-pages}]
   ["/interface/frames/object/:object-id" {:object-id #"\d+"} {:object-id Integer} {:get iframes/edit-object :post iframes/handle-object}]
   ["/interface/frames/object/richtext/:object-id" {:object-id #"\d+"} {:object-id Integer} {:get iframes/richtext}]
   ["/interface/frames/url-picker" {:get iframes/url-picker}]
   ["/interface/frames/file-picker" {:get iframes/file-picker}]
   ["/interface/pages" {:get ipages/get-pages}]
   ["/interface/pages/:id" {:id #"\d+"} {:id Integer} {:get ipages/get-pages}]
   ["/interface/pages/edit-page" {:post ipages/edit-page!}]
   ["/interface/paged/updated-page" {:post ipages/update-page!}]

   ["/interface/objects/delete" {:post iobjects/delete-object!}]
   ["/interface/objects/add" {:post iobjects/add-object!}]
   ["/interface/objects/move" {:post iobjects/move-object!}]
   ["/interface/objects/move-to-area" {:post iobjects/move-object-to-area!}]])
