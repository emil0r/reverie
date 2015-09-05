(ns reverie.site.endpoints.testus
  (:require [reverie.core :refer [defpage]]
            [reverie.cache :as cache]))


(defn testus-rand-fn [request]
  (rand-int 100))

(defn testus-get-fn [request page params]
  (Thread/sleep 5000)
  (str "get foo -> " (cache/skip testus-rand-fn)))
(defn testus-post-fn [request page params]
  "post")


(defpage "/testus"
  {:headers {"Content-Type" "text/plain; charset=utf-8;"}
   :cache {:cache? true}}
  [["/" {:get testus-get-fn :post testus-post-fn}]])
