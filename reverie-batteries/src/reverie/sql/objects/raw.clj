(ns reverie.sql.objects.raw
  (:require [hiccup.util :refer [escape-html]]
            [reverie.core :refer [defobject]]))


(defn raw [{{edit? :edit?} :reverie} object {:keys [text]} params]
  ;; due to raw being so dangerous, we escape the text when in edit? mode
  ;; in case there is content in there that breaks the site
  (if edit?
    (escape-html text)
    text))

(defobject reverie/raw
  {:table "batteries_raw"
   :migration {:path "src/reverie/sql/objects/migrations/raw/"
               :automatic? true}
   :fields {:text {:name "Text"
                   :type :textarea
                   :initial ""}}
   :sections [{:fields [:text]}]}
  {:any raw})
