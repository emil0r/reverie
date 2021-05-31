(ns db
  (:require [ez-database.core :as db]
            [reverie.admin.model.page :as model.page]
            [reverie.migrator :as migrator]
            [reverie.migrator.sql :refer [get-migration-map]]))

(comment
  (let [db (:database @reverie.server/system)
        ds (get-in db [:db-specs :default :datasource])
        mig (:migrator @reverie.server/system)
        mmap (get-migration-map :core :pre db ds "migrations_reverie" "migrations/reverie/postgresql/")]
    ;; (migrator/rollback mig mmap)
    (migrator/migrate mig mmap)
    ;;(reverie.database/get-page db 1)
    ;; (->> (db/query db
    ;;                ^:opts {[:transformation :post] [:page :reverie/page]}
    ;;                {:select [:*]
    ;;                 :from [:view_reverie_page]
    ;;                 :where [:= :version 0]})
    ;;      first
    ;;      #_:page/type)
    )

  (let [db (:database @reverie.server/system)]
    (model.page/get-pages db))

  (let [db (:database @reverie.server/system)]
    (model.page/get-pages db {:parent 1}))


  )
