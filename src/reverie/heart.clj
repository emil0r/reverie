(ns reverie.heart
  (:require [clojure.core.async :as async :refer [go >! <! <!! chan timeout close!]]
            [reverie.beats.objects :as objects]
            [reverie.beats.pages :as pages]))


(defn beat
  "One heartbeat. Returns channel. Send in false to the channel to stop the beat."
  [f ms]
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


(defn start []
  (beat (objects/clear-cut (* 60 20)) (* 60 1000))
  (beat (objects/clear-copy (* 60 20)) (* 60 1000))
  (beat (pages/clear-edit (* 60 20)) (* 60 1000)))
