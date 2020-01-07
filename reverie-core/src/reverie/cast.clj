(ns reverie.cast
  (:require [schema.core :as s])
  (:refer-clojure :exclude [cast]))

(defprotocol ICast
  (cast [to from-str]))

(defmulti class->cast (fn [to _] to))
(defmethod class->cast java.lang.Integer [_ from-str]
  (Integer/parseInt from-str))
(defmethod class->cast java.lang.Float [_ from-str]
  (Float/parseFloat from-str))
(defmethod class->cast java.lang.Double [_ from-str]
  (Double/parseDouble from-str))
(defmethod class->cast java.lang.Long [_ from-str]
  (Long/parseLong from-str))
;; strictly speaking this could hold any type of number,
;; but by convention s/Num is for irrational numbers
(defmethod class->cast java.lang.Number [_ from-str]
  (Double/parseDouble from-str))
(defmethod class->cast s/Int [_ from-str]
  (Long/parseLong from-str))
(defmethod class->cast s/Keyword [_ from-str]
  (keyword from-str))
(defmethod class->cast :default [_ from-str]
  from-str)

(extend-protocol ICast
  java.lang.Class
  (cast [to from-str]
    (class->cast to from-str))
  schema.core.Predicate
  (cast [to from-str]
    (class->cast to from-str)))
