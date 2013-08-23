(ns reverie.util
  (:require [clojure.string :as s]))


(defn kw->str
  "Keep the namespace for keywords"
  [string]
  (-> string str (s/replace #":" "")))

(defn published?
  "object/page == x"
  [x]
  (= 1 (:version x)))

(defn shorten-uri
  "shortens the uri by removing the unwanted part"
  [request remove-part-of-uri]
  (assoc-in request [:uri] (clojure.string/replace
                            (:uri request)
                            (re-pattern (s/replace remove-part-of-uri #"/$" "")) "")))

(defn revmap->kw
  "SQL databases can't store keywords. Do a transformation"
  [m]
  (let [w (merge m (if (:type m) {:type (-> m :type keyword)} {}))
        w (if (:app m) (merge w {:app (-> m :app keyword)}) w)]
    w))

(defn revmap->str
  "SQL databases can't store keywords. Do a transformation"
  [m]
  (let [w (merge m (if (:type m) {:type (-> m :type kw->str)} {}))
        w (if (:app m) (merge w {:app (-> m :app kw->str)}) w)]
    w))


(defn which-version?
  "Which version in the system are we using? 0 for the as of yet unpublished, 1 for published, everything else is a version number in ascending order"
  [request]
  (if (= (:mode request) :edit)
    0
    1))

(defn generate-handler
  "Take handlers, recursively apply to final handler"
  [handlers final-handler]
  (reduce (fn [current [new & args]]
            (apply new current args))
          final-handler
          handlers))

(defn middleware-wrap
  "Wrap middleware in options around the function"
  ([middleware f request args]
     (let [new-f (generate-handler middleware (fn [req]
                                                (f req args)))]
       (new-f request)))
  ([middleware f request args form-data]
     (let [new-f (generate-handler middleware (fn [req]
                                                (f req args form-data)))]
       (new-f request))))
