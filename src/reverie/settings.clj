(ns reverie.settings
  "Namespace for easier handling of reverie settings. Can be directly accessed through @reverie.atoms/settings as well"
  (:refer-clojure :exclude [read write])
  (:use [reverie.atoms :only [settings]]))


(defn read [& path]
  (get-in @settings path))

(defn write! [& path]
  (swap! settings assoc-in (butlast path) (last path)))

(defn delete! [& path]
  (swap! settings update-in (butlast path) dissoc (last path)))
