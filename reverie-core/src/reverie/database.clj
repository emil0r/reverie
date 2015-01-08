(ns reverie.database)


(defprotocol DatabaseProtocol
  (query [database query] [database key? query] [database key query args])
  (query! [database query] [database key? query] [database key query args])
  (databases [database])

  (add-module-entity! [database entity data])
  (update-module-entity! [database entity data])
  (save-module-entity! [database entity data])
  (delete-module-entity! [database entity]))
