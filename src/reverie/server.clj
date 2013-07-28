(ns reverie.server
  (:use [bultitude.core :only [namespaces-on-classpath]]
        [clojure.java.io :only [file]]
        [datomic.api :only [db tempid]])
  (:require [reverie.core :as rev]
            [reverie.middleware :as middleware]))

(defn load-views [& dirs]
  (doseq [f (namespaces-on-classpath :classpath (map file dirs))]
    (require f)))

(defn load-views-ns [& ns-syms]
  (doseq [sym ns-syms
          f (namespaces-on-classpath :prefix (name sym))]
    (require f)))

(defn generate-handler [get-connection handlers {:keys [dev-mode?] :as options}]
  (reduce (fn [current [handler & args]]
            (apply handler current args))
          (fn [request]
            (if-let [[_ route] (rev/get-route (:uri request))]
              (rev/page-render (rev/reverie-data {:connection (get-connection)
                                                  :request request
                                                  :page-type (:page-type route)}))
              {:status 404 :body "404, page not found."}))
          (conj handlers [middleware/reload options])))

(defmulti start :database)
(defmethod start :default [{:keys [port get-connection handlers] :as options}]
  (cond
      (nil? port) (println "No port specified.")
      (nil? get-connection) (println "No get-connection specified.")
      :else (do
              (require 'ring.adapter.jetty)
              (println "Starting server... ")
              (let [jetty-options (merge {:port port :join? false :mode :dev}
                                         (:jetty-options options))
                    run-fn (resolve 'ring.adapter.jetty/run-jetty)]
                (swap! rev/settings options)
                (rev/run-schemas! (get-connection))
                (println (str "Server started on port " port "."))
                (run-fn (generate-handler get-connection handlers options) jetty-options)))))

(defn stop [server]
  (println "Stopping server...")
  (.stop server)
  (println "Done."))

(defn restart [server]
  (println "Restarting server...")
  (.stop server)
  (.start server)
  (println "Done."))
