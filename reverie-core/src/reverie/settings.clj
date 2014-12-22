(ns reverie.settings)

(defprotocol SettingsProtocol
  (sites [settings]))

(defrecord Settings [settings])
