(ns reverie.downstream
  (:refer-clojure :exclude [get assoc! dissoc!]))

(def ^:dynamic *downstream* nil)

(defn assoc! [key value]
  (swap! *downstream* assoc key value))

(defn dissoc!
  ([key]
     (swap! *downstream* dissoc key))
  ([key & ks]
     (apply swap! *downstream* dissoc key ks)))

(defn get
  ([key]
     (clojure.core/get @*downstream* key))
  ([key default]
     (clojure.core/get @*downstream* key default)))
