(ns reverie.page.helpers
  (:require [reverie.atoms :as atoms]))

(defmulti ^:private transform-value (fn [[_ {:keys [type value]}]] [(class value) type]))
(defmethod ^:private transform-value [java.lang.String :boolean] [[key {:keys [value] :as data}]]
  [key (assoc data :value (read-string value))])
(defmethod ^:private transform-value [java.lang.String :number] [[key {:keys [value] :as data}]]
  [key (assoc data :value (read-string value))])
(defmethod ^:private transform-value :default [[key data]]
  [key data])

(defn transform-attributes
  "1. Take attributes from the table page_attributes
2. Keywordize the keys
3. Filter attributes that are not part of the defaults
4. Merge back into defaults and in the process override any defaults that have been changed
5. Transform value to correct type"
  [attributes]
  (let [page-attributes (:page-attributes @atoms/settings)
        ks (keys page-attributes)]
    (into {}
          (map transform-value
               (merge
                page-attributes
                (into {}
                      (map
                       (fn [[key data]]
                         {key (merge (page-attributes key) data)})
                       (filter
                        (fn [[key data]]
                          (some #(= key %) ks))
                        (map (fn [{:keys [key value]}]
                               [(keyword key) {:value value}]) attributes)))))))))
