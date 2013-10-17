(ns reveriedev.objects.text
  (:use [reverie.core :only [defobject]]))



(defobject text {:attributes {:text {:initial ""
                                     :input :richtext
                                     :name "Text"}
                              :a {:initial 0
                                  :input :number
                                  :name "A"}}}
  [:any
   (list
    [:div "object-id->" (get-in request [:reverie :object-id])]
    [:div "params->" (str params)]
    [:div
     text]
    [:div a])])
