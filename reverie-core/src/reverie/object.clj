(ns reverie.object
  (:refer-clojure :exclude [name])
  (:require [clojure.string :as str]
            [reverie.render :as render]
            [reverie.module.entity :as entity]
            [reverie.route :as route]
            [reverie.system :as sys]
            [schema.core :as s]

            reverie.RenderException
            reverie.ObjectException)
  (:import [reverie RenderException ObjectException]
           [reverie.route Route]))


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

(s/defrecord ReverieObject [id :- s/Int
                            name :- s/Keyword
                            area :- s/Keyword
                            order :- s/Int
                            page :- s/Any
                            route :- Route
                            database :- s/Any
                            options :- {s/Any s/Any}
                            methods :- {s/Any s/Any}
                            properties :- {s/Any s/Any}]
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
  (field-attrib [this field attribute]
                (get-in options [:fields field attribute]))
  (field-attrib [this field attribute default]
                (get-in options [:fields field attribute] default))
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
  (publishing? [this] (throw (ObjectException. "IModuleEntity/publishing? is not implemented for reverie.object/ReverieObject")))

  render/IRender
  (render [this {:keys [request-method] :as request}]
          (let [renderer (sys/renderer (:renderer options))
                method (or (get methods request-method)
                           (:any methods))
                out (if (= :app (:type page))
                      (if (or (route/match? route request))
                        (method request this properties (:params request)))
                      (method request this properties (:params request)))]
            (cond
              ;; we have provided methods to the renderer
              (and renderer (:methods-or-routes renderer))
              (render/render renderer request-method out)

              ;; we just want to utilize the render-method
              renderer
              (render/render renderer out)

              ;; we don't want to do anything, just return what we got
              :else
              out)))
  (render [this _ _]
          (throw (RenderException. "[component request sub-component] not implemented for reverie.object/ReverieObject"))))


(defn object [data]
  (if (string? (:route data))
    (map->ReverieObject (assoc data :route (route/route [(:route data)])))
    (map->ReverieObject data)))
