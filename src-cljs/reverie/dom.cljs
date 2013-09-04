(ns reverie.dom
  (:require [jayq.core :as jq]))


(defn uri? [uri]
  (= (-> js/window
         .-location) uri))

(defn $m [sel]
  (-> js/window
      .-parent
      .-main
      .-framem
      .-document
      .-body
      jq/$
      (jq/find sel)))

(defn $o [sel]
  (-> js/window
      .-parent
      .-main
      .-frameo
      .-document
      .-body
      jq/$
      (jq/find sel)))

(defn hide-main []
  (-> js/window
      .-parent
      .-main
      jq/$
      (jq/attr :cols "0,*")))
(defn show-main []
  (-> js/window
      .-parent
      .-main
      jq/$
      (jq/attr :cols "*,0")))

(defn hide-options []
  (-> js/window
      .-parent
      .-main
      jq/$
      (jq/attr :cols "*,0")))
(defn show-options []
  (-> js/window
      .-parent
      .-main
      jq/$
      (jq/attr :cols "0,*")))

(defn options-uri! [uri]
  (set! (-> js/window
            .-parent
            .-main
            .-frameo
            .-location) uri))

(defn main-uri! [uri]
  (set! (-> js/window
            .-parent
            .-main
            .-framem
            .-location) uri))

(defn reload-main! []
  (let [href (-> js/window .-parent .-main .-framem .-location .-pathname)]
    (main-uri! href)))
