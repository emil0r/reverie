(ns reverie.server
  (:use [bultitude.core :only [namespaces-on-classpath]]
        [clojure.java.io :only [file]]
        [noir.cookies :only [wrap-noir-cookies]]
        [noir.session :only [wrap-noir-session mem]]
        [noir.validation :only [wrap-noir-validation]]
        [noir.util.middleware :only [wrap-strip-trailing-slash]]
        [reverie.atoms :only [get-route read-routes!]]
        [reverie.middleware :only [wrap-admin
                                   wrap-edit-mode
                                   wrap-published?
                                   wrap-reverie-data
                                   wrap-redirects]]
        [reverie.role :only [add-roles]]
        [reverie.util :only [generate-handler]]
        [ring.middleware.content-type :only [wrap-content-type]]
        [ring.middleware.file :only [wrap-file]] ;; research for later
        [ring.middleware.file-info :only [wrap-file-info]]
        [ring.middleware.keyword-params :only [wrap-keyword-params]]
        [ring.middleware.multipart-params :only [wrap-multipart-params]]
        [ring.middleware.nested-params :only [wrap-nested-params]]
        [ring.middleware.params :only [wrap-params]]
        [ring.middleware.resource :only [wrap-resource]]
        [ring.middleware.session.memory :only [memory-store]])
  (:require [ez-image.core :as ez]
            [me.raynes.fs :as fs]
            reverie.admin.index
            [reverie.heart :as heart]
            [reverie.page :as page]
            [reverie.response :as r])
  (:import org.apache.commons.io.FilenameUtils))

(defn load-views [& dirs]
  (doseq [f (namespaces-on-classpath :classpath (map file dirs))]
    (require f)))

(defn load-views-ns [& ns-syms]
  (doseq [sym ns-syms
          f (namespaces-on-classpath :prefix (name sym))]
    (require f)))

(defn server-handler [{:keys [handlers store multipart-opts mime-types
                              file-path resource]}]
  (let [file-path (or file-path (FilenameUtils/concat (.toString fs/*cwd*) "media"))
        resource (or resource "public")
        handlers (apply conj (or handlers [])
                        [[wrap-admin]
                         [wrap-published?]
                         [wrap-edit-mode]
                         [wrap-redirects]
                         [wrap-reverie-data]
                         [wrap-content-type]
                         [wrap-keyword-params]
                         [wrap-nested-params]
                         [wrap-params]
                         [wrap-multipart-params multipart-opts]
                         [wrap-file file-path]
                         [wrap-resource resource]
                         [wrap-file-info mime-types]
                         [wrap-strip-trailing-slash]
                         [wrap-noir-validation]
                         [wrap-noir-cookies]
                         [wrap-noir-session {:store (or store (memory-store mem))}]])]
    (generate-handler
     handlers
     (fn [request]
       (let [{{route-data :route-data} :reverie} request]
         (page/render (assoc request :page-type (:page-type route-data))))))))

(defn init []
  (add-roles :edit :publish)
  (fs/mkdirs "media/images")
  (fs/mkdirs "media/files")
  (fs/mkdirs "media/cache/images")
  (ez/setup! "media/cache/images/" "/cache/images/")
  (heart/start))

(defn start [{:keys [port handlers] :as options}]
  (init)
  (cond
      (nil? port) (println "No port specified.")
      :else (do
              (read-routes!)
              (require 'ring.adapter.jetty)
              (println "Starting server... ")
              (let [jetty-options (merge {:port port :join? false}
                                         (:jetty-options options))
                    run-fn (resolve 'ring.adapter.jetty/run-jetty)]
                (println (str "Server started on port " port "."))
                (run-fn (server-handler options) jetty-options)))))

(defn stop [server]
  (println "Stopping server...")
  (.stop server)
  (println "Done."))

(defn restart [server]
  (println "Restarting server...")
  (.stop server)
  (.start server)
  (println "Done."))
