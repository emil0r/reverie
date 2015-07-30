(ns reverie.template
  (:require [reverie.page :as page]
            [reverie.render :as render]
            reverie.RenderException)
  (:import [reverie RenderException]))

(defrecord Template [function]
  render/IRender
  (render [this _]
    (throw (RenderException. "[component request] not implemented for reverie.template/Template")))
  (render [this request page]
    (function request page (page/properties page) (:params request))))


(defn template [function]
  (Template. function))
