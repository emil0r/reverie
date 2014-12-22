(ns reverie.template
  (:require [reverie.render :as render])
  (:import [reverie RenderException]))

(defrecord Template [function]
  render/RenderProtocol
  (render [this _ _]
    (throw (RenderException. "[component request properties] not implemented for reverie.template/Template")))
  (render [this request page properties]
    (function request page properties (:params request))))


(defn template [function]
  (Template. function))
