(ns reverie.object
  (:refer-clojure :exclude [name])
  (:require [reverie.render :as render]
            [reverie.route :as route]
            [reverie.system :as sys])
  (:import [reverie RenderException]))


(defn initial-properties [object-name]
  (let [object-name (keyword object-name)]
   (into {} (map (fn [[k {:keys [initial]}]]
                   {k initial})
                 (-> @sys/storage :objects object-name :options :properties)))))

(defprotocol IObject
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
  IObject
  (id [this] id)
  (area [this] area)
  (name [this] name)
  (order [this] order)
  (page [this] page)
  (options [this] options)
  (properties [this] properties)

  render/IRender
  (render [this {:keys [request-method] :as request}]
    (let [method (or (get methods request-method)
                     (:any methods))]
      (if (= :app (:type page))
        (if (or (route/match? route request))
          (method request this properties (:params request)))
        (method request this properties (:params request)))))
  (render [this _ _]
    (throw (RenderException. "[component request sub-component] not implemented for reverie.object/ReverieObject"))))


(defn object [data]
  (if (string? (:route data))
    (map->ReverieObject (assoc data :route (route/route [(:route data)])))
    (map->ReverieObject data)))
