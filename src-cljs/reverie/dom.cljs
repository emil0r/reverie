(ns reverie.dom
  (:require [jayq.core :as jq]))


(defn $m [sel]
  (-> js/window
      .-parent
      .-main
      .-framem
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
