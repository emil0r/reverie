(ns reverie.object
  (:refer-clojure :exclude [name])
  (:require [clojure.string :as str]
            [reverie.render :as render]
            [reverie.module.entity :as entity]
            [reverie.route :as route]
            [reverie.system :as sys])
  (:import [reverie RenderException ObjectException]))


(defn initial-fields [object-name data]
  (let [object-name (keyword object-name)]
    (into {} (map (fn [[k {:keys [initial]}]]
                    (if (fn? initial)
                      {k (initial data)}
                      {k initial}))
                 (-> @sys/storage :objects object-name :options :fields)))))

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

  entity/IModuleEntity
  (pk [this] (throw (ObjectException. "IModuleEntity/pk is not implemented for reverie.object/ReverieObject")))
  (post-fn [this] (get-in options [:post]))
  (pre-save-fn [this] (get-in options [:pre-save]))
  (fields [this] (:fields options))
  (field [this field] (get-in options [:fields field]))
  (field-options [this field] (get-in options [:fields field]))
  (field-attribs [this field]
    (let [options (get-in options [:fields field])]
      (reduce (fn [out k]
                (if (nil? out)
                  out
                  (if (k options)
                    (assoc out k (k options))
                    out)))
              {}
              [:max :min :placeholder])))
  (field-name [this field]
    (or (get-in options [:fields field :name])
        (-> field clojure.core/name str/capitalize)))
  (error-field-names [this]
    (into {}
          (map (fn [[k opt]]
                 [[k] (or (:name opt)
                          (-> k clojure.core/name str/capitalize))])
               (get-in options [:fields]))))
  (sections [this] (:sections options))
  (slug [this] (throw (ObjectException. "IModuleEntity/slug is not implemented for reverie.object/ReverieObject")))
  (table [this] (throw (ObjectException. "IModuleEntity/table is not implemented for reverie.object/ReverieObject")))

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
