(ns reverie.database)


(defprotocol IDatabase
  (add-page! [database data])
  (update-page! [database id data])
  (save-page-properties! [database serial data])
  (move-page! [database id origo-id movement]) ;; movement = :before/after
  (get-pages [database] [database published?])
  (get-page-with-route [database serial])
  (get-pages-by-route [database])
  (get-page [database id] [database serial published?])
  (get-children [database page] [database serial published?])
  (get-children-count [database page])

  (get-object [database id])
  (get-objects [database page])
  (add-object! [database data])
  (update-object! [database id data])
  (move-object!
    [database id direction] ;; :up/down/bottom/top
    [database id page-id area]) ;; always bottom
  (move-object-to-object! [database id other-id direction]) ;; after/before
)
