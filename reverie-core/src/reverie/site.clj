(ns reverie.site
  (:require [reverie.render :as render]))



;; TODO
;; move roles, pages, objects, etc to here?

(defrecord Site [host]
  render/RenderProtocol
  (render [this request]
    ;; initiate the rendering of the entire page
    ))
