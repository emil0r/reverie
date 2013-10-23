(ns reveriedev.apps.gallery
  (:use [hiccup core page]
        [reverie.core :only [defapp]]))


(defapp gallery {}
  [:get ["/"]
   (html5 "my gallery")]
  [:get ["/:gallery"]
   (html5 gallery)])
