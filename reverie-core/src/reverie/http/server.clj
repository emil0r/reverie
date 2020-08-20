(ns reverie.http.server
  (:require [com.stuartsierra.component :as component]
            [reverie.modules.filemanager :as fm]
            [reverie.render :as render]
            [reverie.settings :as settings]
            [reverie.site :as site]
            [taoensso.timbre :as log]

            ;; middleware
            [reverie.helpers.middleware :refer [create-handler]]
            [reverie.http.middleware :refer [;; wrap-admin
                                             ;; wrap-authorized
                                             authz-exception-handler
                                             authn-exception-handler
                                             default-exception-handler
                                             wrap-downstream
                                             wrap-exceptions
                                             ;; wrap-editor
                                             ;; wrap-error-log
                                             wrap-forker
                                             wrap-i18n
                                             wrap-resources
                                             wrap-reverie-data
                                             wrap-strip-trailing-slash]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.file :refer [wrap-file]] ;; research for later
            [ring.middleware.file-info :refer [wrap-file-info]]
            [ring.middleware.head :refer [wrap-head]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.nested-params :refer [wrap-nested-params]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.session.memory :refer [memory-store]]
            [ring.middleware.stacktrace :refer [wrap-stacktrace]]))


(defn site-route [site]
  (fn [request]
    (render/render site request)))

(defn stop-server [server]
  (when (fn? server)
    (server)))

(defrecord Server [dev? server store run-server stop-server
                   site middleware-options server-options settings
                   site-handlers resource-handlers media-handlers
                   opt-out-handler
                   post-handlers pre-handlers filemanager]
  component/Lifecycle
  (start [this]
    (if server
      this
      (let [resource (or (:resource middleware-options) "public")
            resource-handler (or
                              resource-handlers
                              (create-handler [[wrap-resource resource]
                                               [wrap-file-info (:mime-types middleware-options)]
                                               [wrap-content-type (:content-type-resources middleware-options)]
                                               [wrap-not-modified]]
                                              {:status 404 :body "404, Page Not Found" :headers {}}))
            media-handler (or
                           media-handlers
                           (create-handler [[wrap-file (fm/base-dir filemanager)]
                                            [wrap-file-info (:mime-types middleware-options)]
                                            [wrap-content-type (:content-type-resources middleware-options)]
                                            [wrap-not-modified]]
                                           {:status 404 :body "404, Page Not Found" :headers {}}))
            site-handler (create-handler
                          (or site-handlers
                              (vec
                               (concat
                                post-handlers
                                [[wrap-i18n (:i18n middleware-options)]
                                 [wrap-downstream]
                                 [wrap-reverie-data {:dev? dev?}]
                                 [wrap-content-type (:content-type middleware-options)]
                                 [wrap-content-type]
                                 [wrap-keyword-params]
                                 [wrap-nested-params]
                                 [wrap-params]
                                 [wrap-multipart-params
                                  (:multipart-opts middleware-options)]
                                 [wrap-session
                                  {:store (or store (memory-store))
                                   ;; store session
                                   ;; cookies for 360 days
                                   :cookie-attrs {:max-age (get-in middleware-options [:cookie :max-age] 31104000)}}]
                                 [wrap-cookies]
                                 [wrap-strip-trailing-slash]
                                 [wrap-exceptions {:default default-exception-handler
                                                   :auth/not-authenticated authn-exception-handler
                                                   :auth/not-authorize authz-exception-handler}]
                                 [wrap-resources [[(get-in middleware-options [:resources :media]) media-handler]
                                                  [(get-in middleware-options [:resources :resource]) resource-handler]]]]
                                pre-handlers
                                (if dev?
                                  [[wrap-reload]
                                   [wrap-stacktrace]]))))
                          (site-route site))

            ;; wrap site-handler, resource-handler and media-handler in a handler
            ;; that cycles through all of them in the event that :resources
            ;; are not specified in the settings
            handler (if opt-out-handler
                      (wrap-forker opt-out-handler site-handler resource-handler media-handler)
                      (wrap-forker site-handler resource-handler media-handler))
            server (run-server handler server-options)]
        (log/info (format "Running HTTP server on port %s..." (:port server-options)))
        (assoc this :server server))))
  (stop [this]
    (if-not server
      this
      (do
        (log/info "Stopping HTTP server")
        (stop-server server)
        (assoc this :server nil)))))


(defn get-server [data]
  (map->Server data))
