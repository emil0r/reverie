(ns reverie.render)

(defprotocol IRender
  (render [component request] [component request sub-component]))
