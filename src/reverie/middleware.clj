(ns reverie.middleware
  (:require [ring.middleware.reload :as r-reload]))

(defn reload [handler options]
  (if (:dev-mode? options)
    (r-reload/wrap-reload handler options))
  handler)
