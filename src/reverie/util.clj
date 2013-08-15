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

(defn revmap->kw [m]
  (let [w (merge m (if (:type m) {:type (-> m :type keyword)} {}))
        w (if (:app m) (merge w {:app (-> m :app keyword)}) w)]
    w))

(defn revmap->str [m]
  (let [w (merge m (if (:type m) {:type (-> m :type kw->str)} {}))
        w (if (:app m) (merge w {:app (-> m :app kw->str)}) w)]
    w))


(defn which-version? [request]
  (if (= (:mode request) :edit)
    0
    1))
