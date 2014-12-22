(ns reverie.database)


(defprotocol DatabaseProtocol
  (query [db query] [db query args])
  (query! [db query] [db query args])
  (get-users [db])
  (get-user [db])
  (get-pages [db])
  (get-page [db id])
  (get-page-children [db page])
  (get-page-children-count [db page])
  (get-objects [db page]))
