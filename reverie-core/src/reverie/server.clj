(ns reverie.server
  (:require [com.stuartsierra.component :as component]
            [reverie.modules.filemanager :as fm]
            [reverie.render :as render]
            [reverie.settings :as settings]
            [reverie.site :as site]
            [taoensso.timbre :as log]

            ;; middleware
            [noir.cookies :refer [wrap-noir-cookies]]
            [noir.session :refer [wrap-noir-session wrap-noir-flash mem]]
            [noir.util.middleware :refer [wrap-strip-trailing-slash]]
            [reverie.helpers.middleware :refer [create-handler]]
            [reverie.middleware :refer [wrap-admin
                                        wrap-authorized
                                        wrap-downstream
                                        wrap-editor
                                        wrap-error-log
                                        wrap-forker
                                        wrap-i18n
                                        wrap-resources
                                        wrap-reverie-data]]
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
            [ring.middleware.session.memory :refer [memory-store]]
            [ring.middleware.stacktrace :refer [wrap-stacktrace]]))


(defn site-route [site]
  (fn [request]
    (render/render site request)))

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
                                 [wrap-editor]
                                 [wrap-admin]
                                 [wrap-authorized]
                                 [wrap-reverie-data {:dev? dev?}]
                                 ;; we are not wrapping wrap-anti-forgery or
                                 ;; wrap-csrf-token here because it's taken care of
                                 ;; in reverie.site/render
                                 [wrap-content-type (:content-type middleware-options)]
                                 [wrap-content-type]
                                 [wrap-keyword-params]
                                 [wrap-nested-params]
                                 [wrap-params]
                                 [wrap-multipart-params
                                  (:multipart-opts middleware-options)]
                                 [wrap-noir-flash]
                                 [wrap-noir-session
                                  {:store (or store (memory-store))
                                   ;; store session
                                   ;; cookies for 360 days
                                   :cookie-attrs {:max-age (get-in middleware-options [:cookie :max-age] 31104000)}}]
                                 [wrap-noir-cookies]
                                 [wrap-strip-trailing-slash]
                                 [wrap-error-log dev?]
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
