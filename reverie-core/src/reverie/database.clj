(ns reverie.database)


(defprotocol DatabaseProtocol
  (query [db query] [db key? query] [db key query args])
  (query! [db query] [db key? query] [db key query args])
  (databases [db])
  (get-users [db])
  (get-user [db id])
  (get-pages [db])
  (get-pages-by-route [db])
  (get-page [db id])
  (get-page-children [db page])
  (get-page-children-count [db page])
  (get-objects [db page])

  (add-module-entity! [db entity data])
  (update-module-entity! [db entity data])
  (save-module-entity! [db entity data])
  (delete-module-entity! [db entity]))
