(ns reverie.util
  (:require [clojure.string :as s]))


(defn kw->str [string]
  (-> string str (s/replace #":" "")))

(defn published? [x]
  (= 1 (:version x)))
