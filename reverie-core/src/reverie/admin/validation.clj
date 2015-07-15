(ns reverie.admin.validation
  (:require [reverie.module.entity :as entity]
            vlad))

(defn validate [entity params]
  (let [validations (->> entity
                         (entity/fields)
                         (remove (fn [[k v]]
                                   (= (get params k) :reverie.modules.default/skip)))
                         (map (fn [[k v]]
                                [(condp = (:type v)
                                   :datetime (vlad/matches #"^\d{4,4}-\d{2,2}-\d{2,2} \d{2,2}:\d{2,2}$" [k])
                                   nil)
                                 (:validation v)]))
                         (flatten)
                         (remove nil?)
                         (apply vlad/join))]
    (vlad/validate validations params)))
