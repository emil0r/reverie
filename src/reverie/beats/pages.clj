(ns reverie.beats.pages
  (:require [clj-time.core :as time]
            [korma.core :as k]
            [reverie.atoms :as atoms]))


(defn clear-edit [seconds]
  (fn []
    ;; (let [objects (filter (fn [[_ {:keys [action]}]] (= action :copy))
    ;;                      (get-in @atoms/settings [:edits :objects]))]
    ;;  (doseq [[obj-id {when :time}] objects]
    ;;    (if (time/before? (time/plus when (time/seconds seconds))
    ;;                      (time/now))
    ;;      (swap! atoms/settings update-in [:edits :objects] dissoc obj-id))))
    ))
