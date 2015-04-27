(ns reverie.admin.api
  (:require [reverie.core :refer [defpage]]
            [reverie.admin.api.interface.frames :as iframes]
            [reverie.admin.api.interface.frames.pages :as ifpages]
            [reverie.admin.api.interface.objects :as iobjects]
            [reverie.admin.api.interface.pages :as ipages]))


(defpage "/admin/api" {}
  [["/" {:get ipages/get-pages}]

   ;; frames
   ["/interface/frames/object/:object-id" {:object-id #"\d+"} {:object-id Integer} {:get iframes/edit-object :post iframes/handle-object}]
   ["/interface/frames/richtext/:object-id" {:object-id #"\d+"} {:object-id Integer} {:get iframes/richtext}]
   ["/interface/frames/richtext/:module-p" {:module-p #"\w+"} {:get iframes/richtext}]
   ["/interface/frames/url-picker" {:get iframes/url-picker}]

   ;; frames pages
   ["/interface/frames/pages/add/:parent-serial"
    {:parent-serial #"\d+"} {:parent-serial Integer}
    {:get ifpages/add-page
     :post ifpages/handle-add-page}]
   ["/interface/frames/pages/trash/:page-serial"
    {:page-serial #"\d+"} {:page-serial Integer}
    {:get ifpages/trash-page
     :post ifpages/handle-trash-page}]
   ["/interface/frames/pages/publish/:page-serial"
    {:page-serial #"\d+"} {:page-serial Integer}
    {:get ifpages/publish-page
     :post ifpages/handle-publish-page}]
   ["/interface/frames/pages/meta/:page-serial"
    {:page-serial #"\d+"} {:page-serial Integer}
    {:get ifpages/meta-page
     :post ifpages/handle-meta-page}]

   ;; pages
   ["/interface/pages" {:get ipages/get-pages}]
   ["/interface/pages/:id" {:id #"\d+"} {:id Integer} {:get ipages/get-pages}]
   ["/interface/pages/edit-page" {:post ipages/edit-page!}]
   ["/interface/pages/move" {:post ipages/move-page!}]

   ;; objects
   ["/interface/objects/delete" {:post iobjects/delete-object!}]
   ["/interface/objects/add" {:post iobjects/add-object!}]
   ["/interface/objects/move" {:post iobjects/move-object!}]
   ["/interface/objects/move-to-area" {:post iobjects/move-object-to-area!}]])
