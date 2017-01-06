(ns reverie.query
  "Namespace for manipulating HoneySQL queries"
  (:require [clojure.zip :as zip]
            [reverie.query.zipper :refer [zipper]]))


(defmacro optional
  "Query, predicate and the HoneySQL helper function"
  ([pred? helper]
   `(if ~pred?
      ~helper
      :##nil##))
  ([q pred? helper]
   `(if ~pred?
      (-> ~q ~helper)
      ~q)))

(defn clean
  "Clean a query map from optional :##nil## values"
  [query]
  (loop [loc (zipper query)]
    (let [next-loc (zip/next loc)]
      (if (zip/end? next-loc)
        (zip/root loc)
        (if (= :##nil## (zip/node next-loc))
          (recur (zip/remove next-loc))
          (recur next-loc))))))
