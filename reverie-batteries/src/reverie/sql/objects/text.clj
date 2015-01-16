(ns reverie.sql.objects.text
  (:require [reverie.core :refer [defobject]]))

(defn- text [request object {:keys [text]} params]
  [:p text])

(defobject reverie/text
  {:table "batteries_text"
   :migration {:path "src/reverie/sql/objects/migrations/text/"
               :automatic? true}
   :properties-order [:text]
   :properties {:text {:initial ""
                       :input :richtext
                       :name "Text"}}}
  {:any text})
