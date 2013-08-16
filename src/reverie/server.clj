(ns reverie.server
  (:use [bultitude.core :only [namespaces-on-classpath]]
        [clojure.java.io :only [file]]
        [noir.cookies :only [wrap-noir-cookies]]
        [noir.session :only [wrap-noir-session mem]]
        [reverie.atoms :only [get-route]]
        [reverie.middleware :only [wrap-admin]]
        ;;[ring.middlewar.file :only [wrap-file]] ;; research for later
        [ring.middleware.file-info :only [wrap-file-info]]
        [ring.middleware.keyword-params :only [wrap-keyword-params]]
        [ring.middleware.multipart-params :only [wrap-multipart-params]]
        [ring.middleware.nested-params :only [wrap-nested-params]]
        [ring.middleware.params :only [wrap-params]]
        [ring.middleware.resource :only [wrap-resource]]
        [ring.middleware.session.memory :only [memory-store]])
  (:require [reverie.page :as page]
            [reverie.responses :as r]))

(defn load-views [& dirs]
  (doseq [f (namespaces-on-classpath :classpath (map file dirs))]
    (require f)))

(defn load-views-ns [& ns-syms]
  (doseq [sym ns-syms
          f (namespaces-on-classpath :prefix (name sym))]
    (require f)))

(defn generate-handler [& {:keys [handlers store multipart-opts mime-types]}]
  (let [handlers (apply conj (or handlers [])
                        [[wrap-admin]
                         [wrap-keyword-params]
                         [wrap-nested-params]
                         [wrap-params]
                         [wrap-multipart-params multipart-opts]
                         [wrap-file-info mime-types]
                         [wrap-noir-cookies]
                         [wrap-noir-session {:store (or store (memory-store mem))}]])]
    (reduce (fn [current [handler & args]]
              (apply handler current args))
            (fn [request]
              (if-let [[_ route] (get-route (:uri request))]
                (page/render (assoc request :page-type (:page-type route)))
                r/response-404))
            handlers)))

;; (defmulti start :database)
;; (defmethod start :default [{:keys [port get-connection handlers] :as options}]
;;   (cond
;;       (nil? port) (println "No port specified.")
;;       (nil? get-connection) (println "No get-connection specified.")
;;       :else (do
;;               (require 'ring.adapter.jetty)
;;               (println "Starting server... ")
;;               (let [jetty-options (merge {:port port :join? false :mode :dev}
;;                                          (:jetty-options options))
;;                     run-fn (resolve 'ring.adapter.jetty/run-jetty)]
;;                 (swap! rev/settings options)
;;                 (rev/run-schemas! (get-connection))
;;                 (println (str "Server started on port " port "."))
;;                 (run-fn (generate-handler get-connection handlers options) jetty-options)))))

;; (defn stop [server]
;;   (println "Stopping server...")
;;   (.stop server)
;;   (println "Done."))

;; (defn restart [server]
;;   (println "Restarting server...")
;;   (.stop server)
;;   (.start server)
;;   (println "Done."))
