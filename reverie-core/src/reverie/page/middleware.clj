(ns reverie.page.middleware
  (:require [reverie.render :as render]))



(defn wrap-page-render [page]
  (fn [request]
    (render/render page request)))


