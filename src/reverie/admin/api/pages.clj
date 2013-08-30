(ns reverie.admin.api.pages
  (:require [korma.core :as k]
            [reverie.core :as rev])
  (:use reverie.entity
        [reverie.util :only [published?]]
        [ring.middleware.json :only [wrap-json-params
                                     wrap-json-response]]))

(defn- page->data [p & [lazy?]]
  {:title (:name p)
   :real-title (:title p)
   :uri (:uri p)
   :id (:id p)
   :key (:serial p)
   :serial (:serial p)
   :published? (published? p)
   :isLazy (if (nil? lazy?) true lazy?)
   :created (:created p)
   :updated (:updated p)
   :order (:order p)})

(defn- get-pages [id]
  (let [p (first (k/select page (k/where {:id id})))
        children (k/select page (k/where {:version 0
                                          :parent id}))]
    (if (empty? children)
      (page->data p false)
      (assoc (page->data p) :children (map page->data children)))))


(rev/defpage "/admin/api/pages" {:middleware [[wrap-json-params]
                                              [wrap-json-response]]}
  [:get ["/read"]
   (get-pages (-> (k/select page (k/where {:version 0 :parent 0})) first :id))]
  [:get ["/read/:parent"]
   (get-pages (read-string parent))]
  [:post ["/write" data]
   false]
  [:post ["/add" data]
   false]
  [:post ["/delete" data]
   false]
  [:post ["/search" data]
   false]
  [:post ["/publish" data]
   false]
  [:post ["/unpublish" data]
   false])
