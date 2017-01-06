(ns reverie.query.zipper
  (:require [clojure.zip :as zip])
  (:import [clojure.lang IPersistentVector IPersistentMap IPersistentList ISeq]))


(defmulti branch? class)
(defmethod branch? :default [_] false)
(defmethod branch? IPersistentVector [v] true)
(defmethod branch? IPersistentMap [m] true)
(defmethod branch? IPersistentList [l] true)
(defmethod branch? ISeq [s] true)

(defmulti seq-children class)
(defmethod seq-children IPersistentVector [v] v)
(defmethod seq-children IPersistentMap [m] (mapv identity m))
(defmethod seq-children IPersistentList [l] l)
(defmethod seq-children ISeq [s] s)

(defmulti make-node (fn [node children] (class node)))
(defmethod make-node IPersistentVector [v children]
  (vec children))
(defmethod make-node IPersistentMap [m children]
  (into {} children))
(defmethod make-node IPersistentList [_ children]
  children)
(defmethod make-node ISeq [node children]
  (apply list children))
(prefer-method make-node IPersistentList ISeq)

(defn zipper [node]
  (zip/zipper branch? seq-children make-node node))
