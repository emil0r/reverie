(ns reverie.batteries.breadcrumbs
  (:require [clojure.string :as s])
  (:use [reverie.util :only [join-uri]]))

(defn- get-crumbs [{:keys [crumb-data holder holder-attrib elem elem-attrib
                           href-attrib separator last? parts base-uri]
                    :or {separator " &rsaquo; " last? true holder :ul elem :li
                         base-uri ""}}]
  [holder holder-attrib
   (butlast
    (interleave
     (map (fn [[uri name]]
            [elem elem-attrib
             [:a (merge href-attrib {:href (join-uri base-uri uri)})
              name]]) crumb-data)
     (map (fn [_] [:li.separator separator]) (range (count crumb-data)))))
   (if (and last? (not (empty? crumb-data)))
     [:li.separator separator])
   (if last?
     [elem elem-attrib
      (if (sequential? (last parts)) (second (last parts)) (last parts))])])

(defmulti crumb
  "Takes an URI or a sequence of vectors with [URI part, name of URI]"
  (fn [to-crumb & _] (class to-crumb)))
(defmethod crumb java.lang.String
  ([uri]
     (crumb uri {}))
  ([uri data]
     (let [parts (remove s/blank? (s/split uri #"/"))
           crumbs (reduce (fn [out index]
                            (if (= index (count parts))
                              out
                              (let [crumb (take (+ index 1) parts)]
                                (conj out [(str "/" (s/join "/" crumb))
                                           (last crumb)]))))
                          []
                          (range (count parts)))
           crumb-data (butlast crumbs)]
       {:crumb-data crumb-data
        :crumbs (get-crumbs (merge data {:crumb-data crumb-data :parts parts}))
        :last (last parts)})))
(defmethod crumb clojure.lang.IPersistentVector
  ([uri-data]
     (crumb uri-data {}))
  ([uri-data data]
     (let [parts (remove (fn [[part _]] (s/blank? part)) uri-data)
           crumbs (reduce (fn [out index]
                            (if (= index (count parts))
                              out
                              (let [crumb (take (+ index 1) parts)]
                                (conj out [(str "/" (s/join "/" (map first crumb)))
                                           (second (nth crumb index))]))))
                          []
                          (range (count parts)))
           crumb-data (butlast crumbs)]
       {:crumb-data crumb-data
        :crumbs (get-crumbs (merge data {:crumb-data crumb-data :parts parts}))
        :last (last parts)})))
(defmethod crumb clojure.lang.ISeq
    ([uri-data]
     (crumb (vec uri-data) {}))
  ([uri-data data]
     (crumb (vec uri-data) data)))

(defmethod crumb :default [_ _])
