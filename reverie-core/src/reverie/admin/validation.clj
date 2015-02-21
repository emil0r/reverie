(ns reverie.admin.validation
  (:require [reverie.module.entity :as entity]
            vlad))

(defn- error-item [field errors]
  (map (fn [error]
         [:div.error error])
       errors))

(defn validate [entity params]
  (let [validations (apply vlad/join
                           (remove nil?
                                   (map (fn [[_ v]]
                                          (:validation v))
                                        (entity/fields entity))))]
    (vlad/validate validations params)))
