(ns reverie.admin.modules.validators
  (:require [clojure.string :as s]))


(defn valid-datetime? [value]
  (cond
   (instance? java.sql.Timestamp value) true
   (s/blank? value) true
   :else (re-find #"^\d{4,4}-\d{2,2}-\d{2,2} [0-2][0-9](?::[0-6][0-9]){1,2}$" (s/trim value))))


