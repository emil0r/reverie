(ns reverie.session
  (:require [ring.middleware.session.store :refer [SessionStore]]))



(defrecord MemStore [session-map]
  SessionStore
  (read-session [_ key]
    (get @session-map key))
  (write-session [_ key data]
    (swap! session-map key data))
  (delete-session [_ key]
    (swap! session-map dissoc key)))



(defn mem-store
  ([] (MemStore. (atom {})))
  ([session-map] (MemStore. session-map)))
