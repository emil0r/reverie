(ns reverie.cast
  (:refer-clojure :exclude [cast]))

(defprotocol ICast
  (cast [to from-str]))

(defmulti class->cast (fn [to _] to))
(defmethod class->cast "java.lang.Integer" [_ from-str]
  (Integer/parseInt from-str))
(defmethod class->cast "java.lang.Float" [_ from-str]
  (Float/parseFloat from-str))
(defmethod class->cast "java.lang.Double" [_ from-str]
  (Double/parseDouble from-str))
(defmethod class->cast "java.lang.Long" [_ from-str]
  (Long/parseLong from-str))
(defmethod class->cast :default [_ from-str]
  from-str)

(extend-protocol ICast
  java.lang.Class
  (cast [to from-str]
    (class->cast (.getName to) from-str)))
