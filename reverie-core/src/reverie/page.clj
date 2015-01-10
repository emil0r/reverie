(ns reverie.page
  (:refer-clojure :exclude [type name])
  (:require [clojure.string :as str]
            [reverie.database :as db]
            [reverie.object :as object]
            [reverie.route :as route]
            [reverie.render :as render]
            [reverie.util :as util])
  (:import [reverie RenderException]))

(defprotocol PageDatabaseProtocol
  (get-pages [database] [database published?])
  (get-pages-by-route [database])
  (get-page [database id] [database serial published?])
  (get-children [database page])
  (get-children-count [database page]))


(defprotocol PageProtocol
  (id [page])
  (serial [page])
  (parent [page])
  (root? [page])
  (children [page])
  (children? [page])
  (title [page])
  (name [page])
  (order [page])
  (options [page])
  (properties [page])
  (path [page])
  (objects [page])
  (type [page])
  (version [page])
  (published? [page]))


(defn type? [page expected]
  (= (type page) expected))

(defn- handle-response [options response]
  (cond
   (map? response) response
   (nil? response) response
   :else
   (let [{:keys [headers status]} (:http options)]
     {:status (or status 200)
      :headers (merge {"Content-Type" "text/html; charset=utf-8;"}
                      headers)
      :body response})))

(defrecord Page [route id serial name title properties options template
                 path created updated parent database version
                 published-date published? objects]
  route/RoutingProtocol
  (get-route [this] route)
  (match? [this request] (route/match? route request))

  PageProtocol
  (id [this] id)
  (serial [this] serial)
  (version [this] version)
  (published? [this] (= version 1))
  (parent [this] parent)
  (root? [page] (nil? parent))
  (children [this] (get-children database this))
  (children? [this] (pos? (get-children-count database this)))
  (title [this] title)
  (name [this] name)
  (order [this] order)
  (options [this] options)
  (properties [this] properties)
  (path [this] (:path route))
  (objects [this] (sort-by :order objects))
  (type [page] :page)

  render/RenderProtocol
  (render [this request]
    (handle-response
     options
     (render/render template request this)))
  (render [this _ _]
    (throw (RenderException. "[component request sub-component] not implemented for reverie.page/Page"))))


(defrecord RawPage [route options routes database]
  route/RoutingProtocol
  (get-route [this] route)
  (match? [this request] (route/match? route request))

  PageProtocol
  (id [this])
  (serial [this])
  (version [this])
  (published? [this])
  (parent [this])
  (root? [this] false)
  (children [this])
  (children? [this] false)
  (title [this])
  (name [this])
  (order [this])
  (options [this] options)
  (properties [this] nil)
  (path [this] (:path route))
  (objects [this])
  (type [page] :raw)

  render/RenderProtocol
  (render [this {:keys [request-method] :as request}]
    (let [request (merge request
                         {:shortened-uri (util/shorten-uri
                                          (:uri request) (:path route))})]
      (handle-response
       options
       (if-let [page-route (first (filter #(route/match? % request) routes))]
         (let [{:keys [request method]} (route/match? page-route request)]
           (method request this (:params request)))))))
  (render [this _ _]
    (throw (RenderException. "[component request sub-component] not implemented for reverie.page/RawPage"))))


(defrecord AppPage [route app app-routes app-area-mappings
                    id serial name title properties options template
                    path created updated parent database version
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
  (serial [this] serial)
  (version [this] version)
  (published? [this] (= version 1))
  (parent [this] parent)
  (root? [page] (nil? parent))
  (children [this] (get-children database this))
  (children? [this] (pos? (get-children-count database this)))
  (title [this] title)
  (name [this] name)
  (order [this] order)
  (properties [this] properties)
  (options [this] options)
  (path [this] (:path route))
  (objects [this] (sort-by :order objects))
  (type [page] :app)

  render/RenderProtocol
  (render [this request]
    (let [request (merge request
                         {:shortened-uri (util/shorten-uri
                                          (:uri request) (:path route))})]
      (handle-response
       options
       (if-let [app-route (first (filter #(route/match? % request) app-routes))]
         (let [{:keys [request method]} (route/match? app-route request)
               resp (method request this properties (:params request))]
           (if (map? resp)
             (let [t (or (:template properties) template)]
               (render/render t request (assoc this :rendered resp)))
             resp))))))
  (render [this _ _]
    (throw (RenderException. "[component request sub-component] not implemented for reverie.page/Page"))))


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
