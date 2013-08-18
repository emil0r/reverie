(ns reverie.test.init
  (:use [korma.db :only [defdb postgres]]))


(def db {:classname "org.postgresql.Driver"
         :subprotocol "postgresql"
         :subname "//localhost:5432/dev-reverie"
         :user "dev-reverie"
         :password "reverie"})
(defdb dev-db db)
