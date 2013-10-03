(ns reverie.heart
  (:require [clojure.core.async :as async :refer [go >! <! <!! chan timeout close!]]))


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
