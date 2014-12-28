(ns reverie.user)

(defprotocol UserProtocol
  (id [user]))


(defrecord User [id])
