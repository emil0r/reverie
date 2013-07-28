(ns reverie.middleware
  (:require [ring.middleware.reload :as r-reload]))

(defn dev-mode? [options]
  (= (:mode options) :dev))

(defn reload [handler options]
  (if (dev-mode? options)
    (r-reload/wrap-reload handler options))
  handler)
