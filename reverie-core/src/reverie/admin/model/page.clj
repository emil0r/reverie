(ns reverie.admin.model.page
  (:require [clojure.string :as str]
            [ez-database.core :as db]
            [ez-database.query :as query]
            [ez-database.transform :as transform]
            [reverie.util :refer [kw->str str->kw]]))

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
               [:published_p :page/published?])

(defn get-pages
  ([db]
   (db/query db
             ^:opts {[:transformation :post] [:page :reverie/page]}
             {:select [:*]
              :from [:view_reverie_page]
              :where [:= :version 0]}))
  ([db {:keys [name id serial parent]}]
   (let [q (-> {:select [:*]
                :from [:view_reverie_page]
                :where [:and
                        [:= :version 0]
                        (query/optional name [:search :name name])
                        (query/optional id [:= :id id])
                        (query/optional serial [:= :serial serial])
                        (query/optional parent [:= :parent parent])]}
               (query/clean))]
     (db/query db
               ^:opts {[:transformation :post] [:page :reverie/page]}
               q))))
