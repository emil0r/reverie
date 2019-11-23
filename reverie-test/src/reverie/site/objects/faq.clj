(ns reverie.site.objects.faq
  (:require [reverie.core :refer [defextension]]))

(defextension reverie/faq :object :foobar/extension
  {:table "foobar"
   :migration {:path "reverie/batteries/objects/faq/extended/"
               :automatic? true}
   :fields []})
