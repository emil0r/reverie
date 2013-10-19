(ns reverie.page.helpers
  (:require [reverie.atoms :as atoms]))

(defmulti ^:private transform-attribute (fn [_ _ type] type))
(defmethod ^:private transform-attribute "string" [key value type]
  [(keyword key) value (keyword type)])
(defmethod ^:private transform-attribute :default [key value type]
  [(keyword key) (read-string value) (keyword type)])

(defn transform-attributes
  "1. Take attributes from the table page_attributes
2. Transform to appropriate Clojure values
3. Filter attributes that are not part of the defaults
4. Merge back into defaults and in the process override any defaults that have been changed"
  [attributes]
  (let [page-attributes (:page-attributes @atoms/settings)
        ks (keys page-attributes)]
    (merge
     page-attributes
     (into {}
           (map
            (fn [[key data]]
              {key (merge (page-attributes key) data)})
            (filter
             (fn [[key data]]
               (some #(= key %) ks))
             (map (fn [{:keys [key value type] :as attribute}]
                    (let [[key value type] (transform-attribute key value type)]
                      [key {:value value :type type}])) attributes)))))))
