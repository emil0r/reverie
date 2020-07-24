(ns reverie.session
  (:refer-clojure :exclude [swap! get get-in])
  (:require [reverie.util :refer [atom?]]
            [ring.middleware.session.store :refer [SessionStore]]))



(defrecord MemStore [session-map]
  SessionStore
  (read-session [_ key]
    (clojure.core/get @session-map key))
  (write-session [_ key data]
    (clojure.core/swap! session-map key data))
  (delete-session [_ key]
    (clojure.core/swap! session-map dissoc key)))


(defn- -get-session [request-or-session]
  (if (atom? request-or-session)
    request-or-session
    (clojure.core/get-in request-or-session [:reverie :session])))

(defn mem-store
  ([] (MemStore. (atom {})))
  ([session-map] (MemStore. session-map)))


(defn swap! [request f & args]
  (let [session (-get-session request)]
    (apply clojure.core/swap! session f args)))

(defn assoc-in! [request ks v]
  (let [session (-get-session request)]
    (clojure.core/swap! session assoc-in ks v)))

(defn clear! [request]
  (let [session (-get-session request)]
    (reset! session {})))

(defn get
  ([request k]
   (let [session (-get-session request)]
     (clojure.core/get @session k)))
  ([request k default]
   (let [session (-get-session request)]
     (clojure.core/get @session k default))))

(defn get-in
  ([request ks]
   (let [session (-get-session request)]
     (clojure.core/get-in @session ks)))
  ([request ks default]
   (let [session (-get-session request)]
     (clojure.core/get-in @session ks default))))

(defn put! [request k v]
  (let [session (-get-session request)]
    (clojure.core/swap! session assoc k v)))

(defn remove! [request k v]
  (let [session (-get-session request)]
    (clojure.core/swap! session dissoc k)))
