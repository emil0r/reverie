(ns reveriedev.apps.gallery
  (:use [hiccup core page]
        [reverie.core :only [defapp with-template]]))


(defapp gallery {}
  [:get ["/"]
   (with-template :main request
     {:a "my gallery"
      :b "fufufu"})]
  [:get ["/:gallery"]
   (html5 gallery)])
