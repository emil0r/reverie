(ns reverie.admin.updated
  (:require [reverie.page :as r-page]
            [reverie.object :as r-object]))


(defn via-page [page-id]
  (r-page/updated! {:page-id page-id}))

(defn via-object [object-id]
  (r-page/updated! {:page-id (:page_id (first (r-object/get* {:id object-id})))}))
