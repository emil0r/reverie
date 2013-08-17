(ns reverie.admin.templates
  (:use [hiccup core page]))


(defn- mould-keys [{:keys [title-back] :as m}]
  (-> m
      (assoc :title-back (or title-back "reverie"))))

(defn main [m & body]
  (let [{:keys [title title-back] :as m} (mould-keys m)]
    (html5
     [:head
      [:charset "utf-8"]
      [:title (str title " &mdash; " title-back)]]
     [:body
      body])))
