(ns reverie.render)

(defprotocol RenderProtocol
  (render [component request] [component request sub-component]))
