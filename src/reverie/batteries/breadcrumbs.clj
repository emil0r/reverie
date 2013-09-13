(ns reverie.batteries.breadcrumbs
  (:require [clojure.string :as s]))


(defmulti crumb
  "Takes an URI or a sequence of vectors with [URI part, name of URI]"
  (fn [to-crumb & _] (class to-crumb)))
(defmethod crumb java.lang.String
  ([uri]
     (crumb uri {:separator " › " :last? true}))
  ([uri {:keys [separator last?]}]
     (let [parts (remove s/blank? (s/split uri #"/"))
           crumbs (reduce (fn [out index]
                            (if (= index (count parts))
                              out
                              (let [crumb (take (+ index 1) parts)]
                                (conj out [(str "/" (s/join "/" crumb))
                                           (s/join separator crumb)]))))
                          []
                          (range (count parts)))]
       {:crumbs (if last? crumbs (butlast crumbs))
        :last (last parts)})))
(defmethod crumb clojure.lang.IPersistentVector
  ([uri-data]
     (crumb uri-data {:separator " › " :last? true}))
  ([uri-data {:keys [separator last?]}]
     (let [parts (remove (fn [[part _]] (s/blank? part)) uri-data)
           crumbs (reduce (fn [out index]
                            (if (= index (count parts))
                              out
                              (let [crumb (take (+ index 1) parts)]
                                (conj out [(str "/" (s/join "/" (map first crumb)))
                                           (s/join separator (map second crumb))]))))
                          []
                          (range (count parts)))]
       {:crumbs (if last? crumbs (butlast crumbs))
        :last (last parts)})))
(defmethod crumb clojure.lang.IPersistentList
  ([uri-data]
     (crumb (vec uri-data) {:separator " › " :last? true}))
  ([uri-data data]
     (crumb (vec uri-data) data)))

(defmethod crumb :default [_ _])
