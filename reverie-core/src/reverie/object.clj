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
  (properties [object]))

(defrecord ReverieObject [id name area order page route
                          database properties methods
                          saved-properties]
  ObjectProtocol
  (id [object] id)
  (area [object] area)
  (name [object] name)
  (order [object] order)
  (page [object] page)
  (properties [object] properties)

  render/RenderProtocol
  (render [this {:keys [request-method] :as request}]
    (let [method (or (get methods request-method)
                     (:any methods))]
      (method request this saved-properties (:params request))))
  (render [this _ _]
    (throw (RenderException. "[component request properties] not implemented for reverie.object/ReverieObject")))
  (render [this _ _ _]
    (throw (RenderException. "[component request obj properties] not implemented for reverie.object/ReverieObject"))))


(defn object [data]
  (map->ReverieObject data))
