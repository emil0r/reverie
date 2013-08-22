(ns reverie.dom
  (:require [jayq.core :as jq]))

(defn $c [sel]
  (-> js/window
      .-control
      .-framec
      .-document
      .-body
      jq/$
      (jq/find sel)))


(defn $m [sel]
  (-> js/window
      .-main
      .-framem
      .-document
      .-body
      jq/$
      (jq/find sel)))
