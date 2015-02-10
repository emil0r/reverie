(ns reverie.settings)

(defprotocol ISettings
  (sites [settings]))

(defrecord Settings [settings])
