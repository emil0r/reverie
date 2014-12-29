(ns reverie.object
  (:refer-clojure :exclude [name])
  (:require [reverie.render :as render])
  (:import [reverie RenderException]))

(defprotocol ObjectProtocol
  (id [object])
  (area [object])
  (name [object])
  (order [object])
  (page [object])
  (options [object])
  (properties [object]))

(defrecord ReverieObject [id name area order page route
                          database options methods
                          properties]
  ObjectProtocol
  (id [object] id)
  (area [object] area)
  (name [object] name)
  (order [object] order)
  (page [object] page)
  (options [object] options)
  (properties [object] properties)

  render/RenderProtocol
  (render [this {:keys [request-method] :as request}]
    (let [method (or (get methods request-method)
                     (:any methods))]
      (method request this properties (:params request))))
  (render [this _ _]
    (throw (RenderException. "[component request sub-component] not implemented for reverie.object/ReverieObject"))))


(defn object [data]
  (map->ReverieObject data))
