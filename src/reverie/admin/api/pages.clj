(ns reverie.admin.api.pages
  (:require [korma.core :as k]
            [reverie.core :as rev]
            [reverie.page :as page])
  (:use reverie.entity
        [reverie.util :only [published?]]
        [ring.middleware.json :only [wrap-json-params
                                     wrap-json-response]]))

(defn- page->data [p & [lazy? draggable?]]
  {:title (:name p)
   :real-title (:title p)
   :uri (:uri p)
   :id (:id p)
   :key (:serial p)
   :serial (:serial p)
   :published? (published? p)
   :isLazy (if (nil? lazy?) false lazy?)
   :draggable (if (nil? draggable?) true draggable?)
   :created (:created p)
   :updated (:updated p)
   :order (:order p)
   :parent (:parent p)
   :version (:version p)})

(defn- get-trash []
  {:title "Trash"
   :key "trash"
   :version "trash"
   :draggable false
   :children (map page->data (k/select page
                                       (k/where {:version -1})
                                       (k/order :name :ASC)))})

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
     (and root? (empty? children)) [(page->data p false false) (get-trash)]
     root? [(assoc (page->data p false false) :children children) (get-trash)]
     :else children)))


(rev/defpage "/admin/api/pages" {:middleware [[wrap-json-params]
                                              [wrap-json-response]]}
  [:get ["/read"]
   (get-pages (-> (k/select page (k/where {:version 0 :parent 0})) first :serial) true)]
  [:get ["/read/:parent"]
   (get-pages (read-string parent) false)]
  [:post ["/search" data]
   false]
  [:get ["/move/:node/:source-node/:hit-mode"]
   ;; anchor serial hit-mode in that order
   {:result (page/move! (read-string node) (read-string source-node) hit-mode)}])
