(ns reveriecms.init
  (:require [reverie.server :as server])
  (:use [korma.db :only [defdb postgres]]))

(defdb reveriecms-db (postgres {:db "reveriecms"
                                :user "reveriecms"
                                :password "reveriecms"}))


(defn init []
  (server/load-views "admin"))
