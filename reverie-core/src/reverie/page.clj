(ns reverie.page
  (:refer-clojure :exclude [type name])
  (:require [clojure.core.match :refer [match]]
            [clojure.string :as str]
            [reverie.auth :refer [with-access]]
            [reverie.database :as db]
            [reverie.http.response :as response]
            [reverie.http.route :as route]
            [reverie.object :as object]
            [reverie.page.api :as page.api]
            [reverie.page.app :as page.app]
            [reverie.page.raw-page :as page.raw-page]
            [reverie.page.util :refer [type? handle-response throw-render-exception]]
            [reverie.render :as render]
            [reverie.system :as sys]
            [reverie.util :as util]
            [schema.core :as s]
            [taoensso.timbre :as log]

            [reverie.RenderException])
  (:import [reverie.object ReverieObject]
           [reverie.http.route Route]))


(defprotocol IPage
  (id [page])
  (serial [page])
  (parent [page])
  (root? [page])
  (children [page database])
  (children? [page database])
  (title [page])
  (name [page])
  (order [page])
  (options [page])
  (properties [page])
  (path [page])
  (slug [page])
  (objects [page])
  (type [page])
  (version [page])
  (published? [page])
  (created [page])
  (updated [page])
  (raw [page])
  (cache? [page]))



(s/defrecord Page [route :- Route
                   id :- s/Int
                   serial :- s/Int
                   name :- s/Str
                   title :- s/Str
                   properties :- {s/Any s/Any}
                   template :- (s/either s/Keyword s/Str {s/Keyword s/Any})
                   created :- org.joda.time.DateTime
                   updated :- org.joda.time.DateTime
                   parent :- (s/maybe s/Int)
                   version :- s/Int
                   slug :- s/Str
                   database :- s/Any
                   published-date :- org.joda.time.DateTime
                   published? :- s/Bool
                   objects :- [ReverieObject]
                   raw-data :- s/Any]
  route/IRouting
  (get-route [this] route)
  (match? [this request] (route/match? route request))

  IPage
  (id [this] id)
  (serial [this] serial)
  (version [this] version)
  (published? [this] published?)
  (parent [this] parent)
  (root? [page] (nil? parent))
  (children [this database] (db/get-children database this))
  (children? [this database] (pos? (db/get-children-count database this)))
  (title [this] title)
  (name [this] name)
  (order [this] order)
  (options [this] nil)
  (properties [this] properties)
  (slug [this] slug)
  (path [this] (:path route))
  (objects [this] (sort-by :order objects))
  (type [page] :page)
  (created [page] created)
  (updated [page] updated)
  (raw [page] raw-data)
  (cache? [page] (get-in properties [:cache :cache?]))

  render/IRender
  (render [this request]
    (handle-response
     options
     (render/render template request this)))
  (render [this _ _]
    (throw-render-exception)))


(s/defrecord RawPage [route :- Route
                      options :- {s/Any s/Any}
                      routes :- [s/Any]
                      database :- s/Any]
  route/IRouting
  (get-route [this] route)
  (match? [this request] (route/match? route request))

  IPage
  (id [this] (:path route))
  (serial [this] (:path route))
  (version [this] 1)
  (published? [this] true)
  (parent [this] nil)
  (root? [this] false)
  (children [this database] nil)
  (children? [this database] false)
  (title [this] nil)
  (name [this] nil)
  (order [this] nil)
  (options [this] options)
  (properties [this] nil)
  (slug [this] nil)
  (path [this] (:path route))
  (objects [this] nil)
  (type [page] :raw)
  (created [page] nil)
  (updated [page] nil)
  (raw [page] nil)
  (cache? [page] (get-in options [:cache :cache?]))

  render/IRender
  (render [this request]
    (page.raw-page/render-fn this request))
  (render [this _ _]
    (throw-render-exception)))


(s/defrecord AppPage [route :- Route
                      app :- s/Str
                      app-routes :- [s/Any]
                      app-area-mappings :- s/Any
                      slug :- s/Str
                      id :- s/Int
                      serial :- s/Int
                      name :- s/Str
                      title :- s/Str
                      properties :- {s/Any s/Any}
                      options :- {s/Any s/Any}
                      template :- (s/either s/Keyword s/Str {s/Keyword s/Any})
                      created :- org.joda.time.DateTime
                      updated :- org.joda.time.DateTime
                      parent :- (s/maybe s/Int)
                      database :- s/Any
                      version :- s/Int
                      published-date :- org.joda.time.DateTime
                      published? :- s/Bool
                      objects :- [ReverieObject]
                      raw-data :- s/Any]
  route/IRouting
  (get-route [this] route)
  (match? [this request]
    (with-access
      (get-in request [:reverie :user])
      (:required-roles options)
      (let [pattern (re-pattern (str "^" (:path route)))]
        (if (re-find pattern (:uri request))
          (let [uri (:uri request)
                app-uri (str/replace uri pattern "")
                app-match (first (filter #(route/match? % (assoc request :uri app-uri)) app-routes))]
            (if app-match
              (-> app-match
                  (assoc-in [:request :uri] uri)
                  (assoc-in [:request :app-uri] app-uri))))))))

  IPage
  (id [this] id)
  (serial [this] serial)
  (version [this] version)
  (published? [this] published?)
  (parent [this] parent)
  (root? [page] (nil? parent))
  (children [this database] (db/get-children database this))
  (children? [this database] (pos? (db/get-children-count database this)))
  (title [this] title)
  (name [this] name)
  (order [this] order)
  (properties [this] properties)
  (options [this] options)
  (slug [this] slug)
  (path [this] (:path route))
  (objects [this] (sort-by :order objects))
  (type [page] :app)
  (created [page] created)
  (updated [page] updated)
  (raw [page] raw-data)
  ;; allow overriding options through properties
  (cache? [page] (->> [(get-in properties [:cache :cache?])
                       (get-in options [:cache :cache?])]
                      (remove nil?)
                      first))

  render/IRender
  (render [this request]
    (page.app/render-fn this request))
  (render [this _ _]
    (throw-render-exception)))

(s/defrecord ApiPage [route :- Route
                      options :- {s/Any s/Any}
                      routes :- [s/Any]]
  route/IRouting
  (get-route [this] route)
  (match? [this request] (route/match? route request))

  IPage
  (id [this] (:path route))
  (serial [this] (:path route))
  (version [this] 1)
  (published? [this] true)
  (parent [this] nil)
  (root? [this] false)
  (children [this database] nil)
  (children? [this database] false)
  (title [this] nil)
  (name [this] nil)
  (order [this] nil)
  (options [this] options)
  (properties [this] nil)
  (slug [this] nil)
  (path [this] (:path route))
  (objects [this] nil)
  (type [page] :raw)
  (created [page] nil)
  (updated [page] nil)
  (raw [page] nil)
  (cache? [page] (get-in options [:cache :cache?]))

  render/IRender
  (render [this request]
    (page.api/render-fn this request))
  (render [this _ _]
    (throw-render-exception)))


(defn page [data]
  (if (string? (:route data))
    (map->Page (assoc data :route (route/route [(:route data)])))
    (map->Page data)))

(defn raw-page [data]
  (map->RawPage data))

(defn app-page [data]
  (if (string? (:route data))
    (map->AppPage (assoc data :route (route/route [(:route data)])))
    (map->AppPage data)))

(defn api-page [data]
  (map->ApiPage data))
