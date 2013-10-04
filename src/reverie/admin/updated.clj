(ns reverie.admin.updated
  (:require [reverie.page :as r-page]
            [reverie.object :as r-object]))


(defn page [page-id]
  (r-page/updated! {:page-id page-id}))

(defn object [object-id]
  (r-page/updated! {:page-id (:page_id (r-object/get* {:id object-id}))}))
