(ns reverie.render)

(defprotocol RenderProtocol
  (render [component request] [component request properties] [component request obj properties]))
