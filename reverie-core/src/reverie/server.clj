(ns reverie.server
  (:require [com.stuartsierra.component :as component]
            [ez-image.core :as ez]
            [me.raynes.fs :as fs]
            [noir.cookies :refer [wrap-noir-cookies]]
            [noir.session :refer [wrap-noir-session wrap-noir-flash mem]]
            [noir.util.middleware :refer [wrap-strip-trailing-slash]]
            [reverie.render :as render]
            [reverie.settings :as settings]
            [reverie.site :as site]
            [reverie.middleware :refer [wrap-admin
                                        wrap-error-log
                                        wrap-access
                                        wrap-forker
                                        wrap-reverie-data]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.file-info :refer [wrap-file-info]]
            [ring.middleware.head :refer [wrap-head]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.nested-params :refer [wrap-nested-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.session.memory :refer [memory-store]]
            [ring.middleware.stacktrace :refer [wrap-stacktrace]]
            [ring.util.response :refer [resource-response file-response]]
            [taoensso.timbre :as log]))


(defn- create-handler [handlers routes]
  (reduce (fn [current new]
            (if (nil? new)
              current
              (let [[new & args] new]
                (apply new current args))))
          routes
          handlers))

(defn site-route [site]
  (fn [request]
    (render/render site request)))

(defrecord Server [dev? server store run-server stop-server
                   site resource-routes media-routes
                   middleware-options server-options settings
                   site-handlers resource-handlers media-handlers]
  component/Lifecycle
  (start [this]
    (if server
      this
      (let [resource (or (:resource middleware-options) "public")
            site-handler (create-handler
                          (or site-handlers
                              (vec
                               (concat
                                [[wrap-admin]
                                 [wrap-access]
                                 [wrap-reverie-data]
                                 [wrap-content-type]
                                 [wrap-keyword-params]
                                 [wrap-nested-params]
                                 [wrap-params]
                                 [wrap-multipart-params
                                  (:multipart-opts middleware-options)]
                                 [wrap-noir-session
                                  {:store (or store (memory-store))
                                   ;; store session
                                   ;; cookies for 360 days
                                   :cookie-attrs {:max-age (get-in middleware-options [:cookie :max-age] 31104000)}}]
                                 [wrap-noir-cookies]
                                 [wrap-strip-trailing-slash]
                                 [wrap-error-log dev?]]
                                (if dev?
                                  [[wrap-reload]
                                   [wrap-stacktrace]]
                                  []))))
                          (site-route site))
            resource-handler (create-handler [[wrap-resource resource]
                                              [wrap-file-info (:mime-types middleware-options)]
                                              [wrap-content-type (:content-type middleware-options)]
                                              [wrap-head]]
                                             (resource-response resource))
            handler (wrap-forker site-handler resource-handler)
            server (run-server handler server-options)]
        (log/info (format "Running server on port %s..." (:port server-options)))
        (assoc this :server server))))
  (stop [this]
    (if-not server
      this
      (do
        (log/info "Stopping server")
        (stop-server server)
        (assoc this :server nil)))))


(defn get-server [data]
  (map->Server data))
