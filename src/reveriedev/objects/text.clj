(ns reveriedev.objects.text
  (:use [reverie.core :only [defobject]]))



(defobject text {:attributes {:text {:initial ""
                                     :input :richtext
                                     :name "Text"}}}
  [:any
   (list
    [:div "object-id->" (get-in request [:reverie :object-id])]
    [:div "params->" (str params)]
    [:div
     text])])
