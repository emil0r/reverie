(ns reverie.site.websockets.ws
  (:require [reverie.core :refer [defwebsocket]]
            [reverie.websocket :refer [send!]]))


(defn receive [data]
  (println "receive" data))

(defn close [data]
  (println "close" data))

(defwebsocket "/ws"
  {:on-receive receive
   :on-close close})
