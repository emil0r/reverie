(ns reverie.site.endpoints.cached
  (:require [reverie.core :refer [defpage]]
            [reverie.cache :as cache]))


(defn rand-fn [request]
  (rand-int 100))

(defn cached-rand-get-fn [request page params]
  (Thread/sleep 5000)
  (str "get foo -> " (cache/skip rand-fn)))
(defn cached-rand-post-fn [request page params]
  "post")

(defn cached-rand-static [request page params]
  (str "cached forever -> " (rand-fn request)))


(defpage "/cached"
  {:headers {"Content-Type" "text/plain; charset=utf-8;"}
   :cache {:cache? true}}
  [["/rand" {:get cached-rand-get-fn :post cached-rand-post-fn}]
   ["/static" {:get cached-rand-static}]])
