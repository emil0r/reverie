(ns reverie.util
  (:require [clojure.string :as s]))


(defn kw->str [string]
  (-> string str (s/replace #":" "")))

(defn published? [x]
  (= 1 (:version x)))

(defn shorten-uri [request remove-part-of-uri]
  "shortens the uri by removing the unwanted part"
  (assoc-in request [:uri] (clojure.string/replace
                            (:uri request)
                            (re-pattern (s/replace remove-part-of-uri #"/$" "")) "")))
