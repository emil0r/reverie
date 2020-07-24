(ns reverie.cookies
  (:refer-clojure :exclude [get])
  (:require [reverie.util :refer [atom?]]))


(defn- -get-cookies [request-or-cookies]
  (if (atom? request-or-cookies)
    request-or-cookies
    (get-in request-or-cookies [:reverie :cookies])))


(defn get
  ([request-or-cookies k]
   (let [cookies (-get-cookies request-or-cookies)]
     (clojure.core/get @cookies k)))
  ([request-or-cookies k default]
   (let [cookies (-get-cookies request-or-cookies)]
     (clojure.core/get @cookies k default))))


(defn put!
  ([request-or-cookies k v]
   (let [cookies (-get-cookies request-or-cookies)]
     (swap! cookies assoc k v))))
