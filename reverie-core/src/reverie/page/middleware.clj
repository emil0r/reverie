(ns reverie.page.middleware
  (:require #_[reverie.util :as util]
            [reverie.render :as render]))



(defn wrap-page-render [page]
  (fn [request]
    (render/render page request)))


(defn wrap-shorten-uri [handler page]
  (fn [request]
    (handler (merge request {:shortened-uri (reverie.util/shorten-uri (:uri request) (:path page))}))))
