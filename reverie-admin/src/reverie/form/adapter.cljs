(ns reverie.form.adapter
  (:require [reagent.core :as r]
            [reverie.i18n :refer [t]]))

(defn text-adapter [{:keys [element] :as field}]
  (let [f (r/adapt-react-class element)]
    (fn [{:keys [model placeholder value] :as data}]
      (let [placeholder (if placeholder (t placeholder))]
        [f (merge {:default-value value
                   :placeholder placeholder
                   :on-change #(reset! model (-> % .-target .-value))}
                  (select-keys data [:id]))]))))

