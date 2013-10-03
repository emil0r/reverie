(ns reverie.beats.pages
  (:require [clj-time.core :as time]
            [korma.core :as k]
            [reverie.atoms :as atoms]))


(defn clear-edit [seconds]
  (fn []
    (let [pages (dissoc (:edits @atoms/settings) :objects)
          now (time/now)]
      (doseq [[uri {when :time}] pages]
        (if (time/before? (time/plus when (time/seconds seconds))
                          now)
          (swap! atoms/settings update-in [:edits] dissoc uri))))))
