(ns reverie.page
  (:refer-clojure :exclude [type name])
  (:require [clojure.string :as str]
            [reverie.database :as db]
            [reverie.object :as object]
            [reverie.route :as route]
            [reverie.render :as render])
  (:import [reverie RenderException]))


(defprotocol PageProtocol
  (id [page])
  (parent [page])
  (root? [page])
  (children [page])
  (children? [page])
  (title [page])
  (name [page])
  (order [page])
  (properties [page])
  (path [page])
  (objects [page])
  (type [page]))


(defn type? [page expected]
  (= (type page) expected))

(defrecord Page [route id name title properties template
                 uri created updated parent database
                 published-date published? objects]
  route/RoutingProtocol
  (get-route [this] route)
  (match? [this request] (route/match? route request))

  PageProtocol
  (id [this] id)
  (parent [this] parent)
  (root? [page] (nil? parent))
  (children [this] (db/get-page-children database this))
  (children? [this] (pos? (db/get-page-children-count database this)))
  (title [this] title)
  (name [this] name)
  (order [this] order)
  (properties [this] properties)
  (path [this] (:path route))
  (objects [this] (sort-by :order objects))
  (type [page] :page)

  render/RenderProtocol
  (render [this request]
    (render/render template request this (properties this)))
  (render [this _ _]
    (throw (RenderException. "[component request properties] not implemented for reverie.page/Page")))
  (render [this _ _ _]
    (throw (RenderException. "[component request obj properties] not implemented for reverie.page/Page"))))


(defrecord RawPage [route properties methods database]
  route/RoutingProtocol
  (get-route [this] route)
  (match? [this request] (route/match? route request))

  PageProtocol
  (id [this])
  (parent [this])
  (root? [this] false)
  (children [this])
  (children? [this] false)
  (title [this])
  (name [this])
  (order [this])
  (properties [this] properties)
  (path [this] (:path route))
  (objects [this])
  (type [page] :raw)

  render/RenderProtocol
  (render [this {:keys [request-method] :as request}]
    (let [method (or (get methods request-method)
                     (:any methods))]
      (method request this (properties this) (:params request))))
  (render [this _ _]
    (throw (RenderException. "[component request properties] not implemented for reverie.page/RawPage")))
  (render [this _ _ _]
    (throw (RenderException. "[component request obj properties] not implemented for reverie.page/RawPage"))))


(defrecord AppPage [route app app-routes app-area-mappings
                    id name title properties template
                    uri created updated parent database
                    published-date published? objects]
  route/RoutingProtocol
  (get-route [this] route)
  (match? [this request]
    (let [pattern (re-pattern (str "^" (:path route)))]
      (if (re-find pattern (:uri request))
        (let [uri (:uri request)
              app-uri (str/replace uri pattern "")
              app-match (first (filter #(route/match? % (assoc request :uri app-uri)) app-routes))]
          (if app-match
            (-> app-match
                (assoc-in [:request :uri] uri)
                (assoc-in [:request :app-uri] app-uri)))))))

  PageProtocol
  (id [this] id)
  (parent [this] parent)
  (root? [page] (nil? parent))
  (children [this] (db/get-page-children database this))
  (children? [this] (pos? (db/get-page-children-count database this)))
  (title [this] title)
  (name [this] name)
  (order [this] order)
  (properties [this] properties)
  (path [this] (:path route))
  (objects [this] (sort-by :order objects))
  (type [page] :app)

  render/RenderProtocol
  (render [this request]
    (if-let [app-route (first (filter #(route/match? % request) app-routes))]
      (let [{:keys [request method]} (route/match? app-route request)
            resp (method request this properties (:params request))]
        (if (map? resp)
          (let [t (or (:template properties) template)]
            (render/render t request (assoc this :rendered resp) properties))
          resp))))
  (render [this _ _]
    (throw (RenderException. "[component request properties] not implemented for reverie.page/Page")))
  (render [this _ _ _]
    (throw (RenderException. "[component request obj properties] not implemented for reverie.page/Page"))))


(defn page [data]
  (map->Page data))

(defn raw-page [data]
  (map->RawPage data))

(defn app-page [data]
  (map->AppPage data))
