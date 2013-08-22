(ns reverie.admin.templates
  (:use [hiccup core page]))


(defn- mould-keys [{:keys [title-back] :as m}]
  (-> m
      (assoc :title-back (or title-back "reverie"))))

(defn- get-title [m]
  (str (:title m) " &mdash; " (:title-back m)))

(defn- get-body [body]
  [:body body])

(defn includes [m]
  (reduce (fn [out k]
            (if (-> m k nil?)
              out
              (cond
               (= k :css) (conj out (map include-css (:css m)))
               (= k :js) (conj out (map include-js (:js m)))
               (= k :head) (apply conj out (reverse (:head m))))))
          []
          (keys m)))

(defn main [m & body]
  (let [m (mould-keys m)]
    (html5
     [:head
      [:meta {:charset "utf-8"}]
      [:title (get-title m)]
      (includes m)]
     body)))


(defn auth [m & body]
  (let [m (mould-keys m)]
    (html5
     [:head
      [:meta {:charset "utf-8"}]
      [:title (get-title m)]]
     (get-body body))))


(defn frame-left [m & body]
  (let [m (mould-keys m)]
    (html5
     [:head
      [:meta {:charset "utf-8"}]
      [:title ""]
      (includes m)]
     (get-body body))))
