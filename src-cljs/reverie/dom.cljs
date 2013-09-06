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

(defn $m-ready [func]
  (-> js/window
      .-parent
      .-main
      .-framem
      .-document
      jq/$
      (.ready func)))

(defn $m-loaded [func]
  (-> js/window
      .-parent
      .-document
      jq/$
      (jq/find :#framem)
      (jq/on :load func)))

(defn $m-html []
  (-> js/window
      .-parent
      .-main
      .-framem
      .-document
      .-body
      jq/$
      jq/parent))

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

(defn ^:export reload-main! []
  (let [href (-> js/window .-parent .-main .-framem .-location .-pathname)]
    (main-uri! href)))

