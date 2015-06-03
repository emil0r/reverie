(ns reverie.sql.objects.raw
  (:require [reverie.core :refer [defobject]]))


(defn raw [request object {:keys [text] :as properties} params]
  text)

(defobject raw
  {:table "batteries_raw"
   :migration {:path "src/reverie/sql/objects/migrations/image/"
               :automatic? true}
   :fields {:text {:name "Text"
                   :type :textarea
                   :initial ""}}
   :sections [{:fields [:text]}]}
  {:any raw})
