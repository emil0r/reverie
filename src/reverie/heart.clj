(ns reverie.heart
  (:require [clojure.core.async :as async :refer [go >! <! <!! chan timeout close!]]
            [clj-time.core :as time]
            [reverie.atoms :as atoms]))


(defn beat [f ms]
  (let [c (chan)
        continue? (atom true)]
    (go (while @continue?
          (let [v (<! c)]
            (if v
              (do
                (f)
                (go
                 (<! (timeout ms))
                 (>! c true)))
              (do
                (reset! continue? false)
                (close! c))))))
    (go (>! c true))
    c))
