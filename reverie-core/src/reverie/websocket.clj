(ns reverie.websocket
  (:require [org.httpkit.server :as server]
            [reverie.system :as sys]))

(defonce channels (atom {}))

(defn get-channel [path]
  (get-in @sys/storage [:websockets path :channel]))

(defn close-channel! [path]
  (if-let [channel (get-channel path)]
    (server/close channel)))

(defn send! [path data]
  (if-let [channel (get-channel path)]
    (server/send! channel data)))
