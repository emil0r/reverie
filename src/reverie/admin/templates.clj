(ns reverie.admin.templates
  (:use [hiccup core page]))


(defn- mould-keys [{:keys [title-back] :as m}]
  (-> m
      (assoc :title-back (or title-back "reverie"))))

(defn- get-title [m]
  (str (:title m) " &mdash; " (:title-back m)))

(defn- get-body [body & [opt]]
  [:body [:div.container opt body]])

(defn- custom-js [js]
  [:script {:type "text/javascript"}
   (str "$(document).ready(function(){"
        (apply str js)
        "});")])

(defn includes [m]
  (reduce (fn [out k]
            (if (-> m k nil?)
              out
              (cond
               (= k :css) (apply conj out (reverse (apply include-css (:css m))))
               (= k :js) (apply conj out (reverse (apply include-js (:js m))))
               (= k :custom-js) (conj out (custom-js (:custom-js m)))
               (= k :head) (apply conj out (reverse (:head m))))))
          (list)
          [:custom-js :head :js :css]))

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


(defn frame [m & body]
  (let [m (mould-keys m)]
    (html5
     [:head
      [:meta {:charset "utf-8"}]
      [:title (or (:title m) "")]
      (includes m)]
     (get-body body {:class "frame container"}))))
