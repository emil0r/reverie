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
   :isLazy (if (nil? lazy?) false lazy?)
   :created (:created p)
   :updated (:updated p)
   :order (:order p)})

(defn- get-pages [serial root?]
  (let [p (first (k/select page (k/where {:serial serial :version 0})))
        children (k/select page (k/where {:version 0
                                          :parent (:serial p)}))
        grand-children (k/select page (k/where {:version 0
                                                :parent [in (map :serial children)]}))
        children (map (fn [{:keys [serial] :as c}]
                        (page->data
                         c
                         (some #(= (:parent %) serial)
                               grand-children))) children)]
    (cond
     (and root? (empty? children)) (page->data p false)
     root? (assoc (page->data p) :children children)
     :else children)))


(rev/defpage "/admin/api/pages" {:middleware [[wrap-json-params]
                                              [wrap-json-response]]}
  [:get ["/read"]
   (get-pages (-> (k/select page (k/where {:version 0 :parent 0})) first :serial) true)]
  [:get ["/read/:parent"]
   (get-pages (read-string parent) false)]
  [:post ["/write" data]
   false]
  [:post ["/add" data]
   false]
  [:post ["/delete" data]
   false]
  [:post ["/search" data]
   false])
