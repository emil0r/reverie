(ns reverie.db
  (:require [datascript.core :as d]
            [re-posh.core :refer [connect!]]))

(def conn (d/create-conn {}))
(connect! conn)
(def default-db {})
