(ns reverie.util
  (:require [clojure.string :as str]))

(defn shorten-uri
  "shortens the uri by removing the unwanted part. Used for defpage and defapp"
  [uri to-remove]
  (if (= to-remove "/")
    uri
    (let [uri (str/replace
               uri (re-pattern (str/replace to-remove #"/$" "")) "")]
      (if (str/blank? uri)
        "/"
        uri))))
