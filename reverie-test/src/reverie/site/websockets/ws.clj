(ns reverie.site.websockets.ws
  (:require [reverie.core :refer [defws]]))


(defn receive [data]
  (println "receive" data))

(defn close [data]
  (println "close" data))

(defws "/ws"
  {:on-receive receive
   :on-close close})
