(ns reverie.admin.api.pages
  (:require [korma.core :as k]
            [reverie.auth.user :as user]
            [reverie.core :as rev]
            [reverie.atoms :as atoms]
            [reverie.page :as page])
  (:use reverie.entity
        [reverie.middleware :only [wrap-access]]
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
                                          :parent (:serial p)})
                           (k/order :order :ASC))
        grand-children (k/select page (k/where {:version 0
                                                :parent [in (map :serial children)]})
                                 (k/order :order :ASC))
        children (map (fn [{:keys [serial] :as c}]
                        (page->data
                         c
                         (some #(= (:parent %) serial)
                               grand-children))) children)]
    (cond
     (and root? (empty? children)) [(page->data p false false) (get-trash)]
     root? [(assoc (page->data p false false) :children children) (get-trash)]
     :else children)))

(defn- get-parent [serial]
  (:parent (first (k/select page (k/where {:serial serial})))))

(defn- get-search-path
  "Add first serial in out if you want it as part of the search path"
  [serial out]
  (let [parent (get-parent serial)]
    (if (or (nil? parent) (zero? parent))
      (vec (reverse out))
      (recur parent (conj out parent)))))


(rev/defpage "/admin/api/pages" {:middleware [[wrap-json-params]
                                              [wrap-json-response]
                                              [wrap-access :edit]]}
  [:get ["/read"]
   (get-pages (-> (k/select page (k/where {:version 0 :parent 0})) first :serial) true)]
  [:get ["/read/:parent"]
   (get-pages (read-string parent) false)]
  [:post ["/search" {:keys [serial]}]
   (let [serial (read-string serial)]
     {:path (get-search-path serial [serial])
      :result true})]
  [:get ["/view"]
   (let [u (user/get)]
     (do
       (atoms/view! (:name (user/get)))
       {:result true}))]
  [:get ["/edit/:serial"]
   (let [p (page/get {:serial (read-string serial) :version 0})
         u (user/get)
         user-name (:name u)]
     (do
       (atoms/view! user-name)
       (atoms/edit! (:uri p) user-name)
       {:result true}))]
  [:get ["/move/:node/:source-node/:hit-mode"]
   ;; anchor serial hit-mode in that order
   {:result (page/move! (read-string node) (read-string source-node) hit-mode)}])
