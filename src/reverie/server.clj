(ns reverie.server
  (:use [datomic.api :only [db tempid]])
  (:require [reverie.core :as rev]))

(defn generate-handler [get-connection handlers]
  (reduce (fn [current [handler & args]]
            (apply handler current args))
   (fn [request]
     (if-let [[_ route] (rev/get-route (:uri request))]
       (rev/page-render (rev/reverie-data {:connection (get-connection)
                                           :request request
                                           :page-type (:page-type route)}))
       {:status 404 :body "404, page not found."}))
   handlers))

(defmulti start :database)
(defmethod start :default [{:keys [port get-connection handlers] :as options}]
  (require 'ring.adapter.jetty)
  (println "Starting server... ")
  (let [jetty-options (merge {:port port :join? false} (:jetty-options options))
        run-fn (resolve 'ring.adapter.jetty/run-jetty)]
    (swap! rev/settings options)
    (rev/run-schemas! (get-connection))
    (println (str "Server started on port " port "."))
    (run-fn (generate-handler get-connection handlers) jetty-options)))

(defn stop [server]
  (println "Stopping server...")
  (.stop server)
  (println "Done."))

(defn restart [server]
  (println "Restarting server...")
  (.stop server)
  (.start server)
  (println "Done."))
