(ns reverie.database)


(defprotocol DatabaseProtocol
  (query [db query] [db query args])
  (query! [db query] [db query args])
  (get-users [db])
  (get-user [db id])
  (get-pages [db])
  (get-pages-by-route [db])
  (get-page [db id])
  (get-page-children [db page])
  (get-page-children-count [db page])
  (get-objects [db page]))
