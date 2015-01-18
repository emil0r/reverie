(ns reverie.database)


(defprotocol DatabaseProtocol
  (query [database query] [database key? query] [database key query args])
  (query! [database query] [database key? query] [database key query args])
  (databases [database])

  (add-page! [database data])
  (update-page! [database id data])
  (get-pages [database] [database published?])
  (get-pages-by-route [database])
  (get-page [database id] [database serial published?])
  (get-children [database page])
  (get-children-count [database page])

  (get-objects [database page])
  (add-object! [database data])
  (update-object! [database id data])

  (add-module-entity! [database entity data])
  (update-module-entity! [database entity data])
  (save-module-entity! [database entity data])
  (delete-module-entity! [database entity]))
