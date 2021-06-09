(ns reverie.admin.model.page
  (:require [clojure.core.match :refer [match]]
            [clojure.string :as str]
            [ez-database.core :as db]
            [ez-database.query :as query]
            [ez-database.transform :as transform]
            [reverie.admin.api.editors :as editors]
            [reverie.admin.http-responses :as http]
            [reverie.auth :as auth]
            [reverie.database :as reverie.db]
            [reverie.page :as page]
            [reverie.util :refer [kw->str str->kw]]
            [taoensso.timbre :as log]))

(defn ->kw [k k-ns]
  (fn [old new]
    (let [v (get old k)]
      (if (str/blank? v)
        (assoc new k-ns nil)
        (assoc new k-ns (str->kw v))))))

(defn <-kw [k k-ns]
  (fn [old new]
    (let [v-ns (get old k-ns)]
      (if (nil? v-ns)
        (assoc new k "")
        (assoc new k (kw->str v-ns))))))

(transform/add :page :reverie/page
               [:id :page/id]
               [:serial :page/serial]
               [:parent :page/parent]
               [:template (->kw :template :page/template) (<-kw :template :page/template) :page/template]
               [:title :page/title]
               [:name :page/name]
               [:properties :page/properties]
               [:updated :page/updated]
               [:created :page/created]
               [:type (->kw :type :page/type) (<-kw :type :page/type) :page/type]
               [:app (->kw :app :page/app) (<-kw :app :page/app) :page/app]
               [:order :page/order]
               [:slug :page/slug]
               [:status :page/status])

(def page-opts ^:opts {[:transformation :post] [:page :reverie/page]})

(defn get-pages
  ([db]
   (db/query db page-opts
             {:select [:*]
              :from [:reverie_page_view]
              :where [:in :version [-1 0]]}))
  ([db {:keys [name id serial serials parent versions] :or {versions [-1 0]}}]
   (let [q (-> {:select [:*]
                :from [:reverie_page_view]
                :where [:and
                        [:in :version versions]
                        (query/optional name [:search :name name])
                        (query/optional id [:= :id id])
                        (query/optional serial [:= :serial serial])
                        (query/optional serials [:in :serial serials])
                        (query/optional parent [:= :parent parent])]}
               (query/clean))]
     (db/query db page-opts q))))

(defn get-page
  ([db serial]
   (get-page db serial 0))
  ([db serial version]
   (->> {:select [:*]
         :from [:reverie_page_view]
         :where [:and
                 [:= :version version]
                 [:= :serial serial]]}
        (db/query db page-opts))))

(defn edit-page [db user {:keys [serial edit?] :as _data}]
  (let [page (reverie.db/get-page db serial false)]
    (match [(auth/authorize? page user db "edit") ;; are we authorized to edit the page?
            edit? ;; do we want to edit the page?
            ]
           [false _] http/no-page-rights
           [_ true] (do (editors/edit! page user)
                        http/success)
           [_ false] (do (editors/stop-edit! user)
                         http/success))))


(defn move-page [db user {:keys [serial origo-serial movement] :as _data}]
  (let [page (reverie.db/get-page db serial false)
        origo (reverie.db/get-page db origo-serial false)]
    (if (and (auth/authorize? page user db "edit")
             (auth/authorize? origo user db "edit"))
      (try
        (reverie.db/move-page! db (page/id page) (page/id origo) movement)
        http/success
        (catch Exception e
          (log/warn e)
          http/no-page-rights))
      http/no-page-rights)))



(comment
  (let [db (:database @reverie.server/system)]
    (get-pages db {:serials [1 2 3]}))
  )
