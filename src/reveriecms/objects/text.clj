(ns reveriecms.objects.text
  (:use [reverie.core :only [defobject]]))



(defobject text {:attributes {:text {:initial ""
                                     :input :text
                                     :name "Text"}}}
  [:any]
  (str "my text->" text))
