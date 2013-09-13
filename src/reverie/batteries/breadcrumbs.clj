(ns reverie.batteries.breadcrumbs
  (:require [clojure.string :as s]))


(defmulti crumb
  "Takes an URI or a sequence of vectors with [URI part, name of URI]"
  (fn [to-crumb & _] (class to-crumb)))
(defmethod crumb java.lang.String
  ([uri separator]
     (crumb uri separator true))
  ([uri separator last?]
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
  ([uri-data separator]
     (crumb uri-data separator true))
  ([uri-data separator last?]
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
  ([uri-data separator]
     (crumb (vec uri-data) separator true))
  ([uri-data separator last?]
     (crumb (vec uri-data) separator last?)))

(defmethod crumb :default [_ _])
