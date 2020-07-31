(ns reverie.page
  (:refer-clojure :exclude [type name])
  (:require [clojure.core.match :refer [match]]
            [clojure.string :as str]
            [reverie.auth :refer [with-access]]
            [reverie.database :as db]
            [reverie.http.response :as response]
            [reverie.http.route :as route]
            [reverie.object :as object]
            [reverie.page.rawpage :as page.rawpage]
            [reverie.page.util :refer [handle-response]]
            [reverie.render :as render]
            [reverie.system :as sys]
            [reverie.util :as util]
            [schema.core :as s]
            [taoensso.timbre :as log]

            [reverie.RenderException])
  (:import [reverie RenderException]
           [reverie.object ReverieObject]
           [reverie.http.route Route]))


(defprotocol IPage
  (id [page] "The id of the page")
  (serial [page] "The serial of the page (this is what ties different versions of the same page together)")
  (parent [page] "The parent of the page, if any")
  (root? [page] "Is this page the root?")
  (children [page database] "Any children of the page")
  (children? [page database] "Does the page have children?")
  (title [page] "The title of the page")
  (name [page] "The name of the page")
  (order [page] "The order of the page with regards to any siblings")
  (options [page] "The options of the page")
  (properties [page] "The properties of the page")
  (path [page] "The URI path of the page")
  (slug [page] "The slug of the page")
  (objects [page] "Any objects belonging the page")
  (type [page] "What type of page")
  (version [page] "Which version of the page (-1 deleted, 0 unpublished, 1 published, 2+ older versions")
  (published? [page] "Is the page published?")
  (created [page] "When was the page created")
  (updated [page] "When was the page updated")
  (raw [page] "Get the raw data of the page (no rendering)")
  (cache? [page] "Is the page cached?"))


(defn type? [page expected]
  (= (type page) expected))

(s/defrecord Page [route :- Route
                   id :- s/Int
                   serial :- s/Int
                   name :- s/Str
                   title :- s/Str
                   properties :- {s/Any s/Any}
                   template :- (s/either s/Keyword s/Str {s/Keyword s/Any})
                   created :- java.time.Instant
                   updated :- java.time.Instant
                   parent :- (s/maybe s/Int)
                   version :- s/Int
                   slug :- s/Str
                   database :- s/Any
                   published-date :- java.time.Instant
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
    (throw (RenderException. "[component request sub-component] not implemented for reverie.page/Page"))))


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
    (page.rawpage/render-page this request))
  (render [this _ _]
    (throw (RenderException. "[component request sub-component] not implemented for reverie.page/RawPage"))))


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
                      created :- java.time.Instant
                      updated :- java.time.Instant
                      parent :- (s/maybe s/Int)
                      database :- s/Any
                      version :- s/Int
                      published-date :- java.time.Instant
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
    (let [request (merge request
                         {:shortened-uri (util/shorten-uri
                                          (:uri request) (:path route))})]
      (handle-response
       options
       (if-let [app-route (first (filter #(route/match? % request) app-routes))]
         (let [{:keys [request method]} (route/match? app-route request)
               renderer (sys/renderer (:renderer options))
               resp (method request this properties (:params request))
               t (or (:template properties) template)]
           (match [(not (nil? renderer)) (not (nil? (:methods-or-routes renderer))) (map? resp) (not (nil? t))]
                  ;; ~renderer, _ , map, template
                  [false _ true true] (render/render t request (assoc this :rendered resp))

                  ;; renderer, routes, map, template
                  [true true true true] (let [out (render/render renderer (:request-method request)
                                                                 {:data resp
                                                                  ::render/type :page/routes
                                                                  :meta {:route-name (get-in app-route [:options :name])}})]
                                          (render/render t request (assoc this :rendered out)))

                  ;; renderer, ~routes, map, template
                  [true false true true] (let [out (render/render renderer (:request-method request)
                                                                  {:data resp
                                                                   ::render/type :page/no-routes
                                                                   :meta {:route-name (get-in app-route [:options :name])}})]
                                           (render/render t request (assoc this :rendered out)))

                  ;; _, _, ~map, template
                  ;; invalid combiation
                  ;; when a template is being used there has to be a map of some sort
                  [_ _ false true] (do (log/error {:what ::AppPage
                                                   :message "Invalid combitation rendering AppPage. Tried to render a template without a corresponding map"
                                                   :template t
                                                   :route route
                                                   :app-route app-route
                                                   :response resp})
                                       (response/raise 500))
                  ;; default
                  [_ _ _ _] resp))))))
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
