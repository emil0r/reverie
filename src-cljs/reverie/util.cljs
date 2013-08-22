(ns reverie.util)

(defn log [& r]
  (map #(.log js/console %) r))
