(ns reverie.object
  (:refer-clojure :exclude [name])
  (:require [reverie.render :as render]
            [reverie.route :as route])
  (:import [reverie RenderException]))

(defprotocol ObjectDatabaseProtocol
  (get-objects [database page]))

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
  (id [this] id)
  (area [this] area)
  (name [this] name)
  (order [this] order)
  (page [this] page)
  (options [this] options)
  (properties [this] properties)

  render/RenderProtocol
  (render [this {:keys [request-method] :as request}]
    (let [method (or (get methods request-method)
                     (:any methods))]
      (method request this properties (:params request))))
  (render [this _ _]
    (throw (RenderException. "[component request sub-component] not implemented for reverie.object/ReverieObject"))))


(defn object [data]
  (if (string? (:route data))
    (map->ReverieObject (assoc data :route (route/route [(:route data)])))
    (map->ReverieObject data)))
