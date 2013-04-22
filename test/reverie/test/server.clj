(ns reverie.test.server
  (:require [reverie.core :as rev]
            [reverie.server :as server]
            [reverie.test.core :as test])
  (:use midje.sweet
        [datomic.api :only [q db] :as d]
        [ring.mock.request]))

(defn get-connection []
  (:connection (test/setup)))

(defn wrap-count [handler]
  (fn [request]
    (let [response (handler request)
          level (get-in response [:headers "level"] 1)]
      (assoc-in response [:headers "level"] (+ level 1)))))

(fact
 "generate-handler"
 (get-in ((server/generate-handler get-connection [[wrap-count] [wrap-count]])
          {:level 1 :uri "/generate-handler"}) [:headers "level"]) => 3)

(fact
 "start server, stop server, restart server"
 (let [s (server/start {:port 8888 :get-connection get-connection :handlers []})]
   (server/restart s)
   (server/stop s)
   s) => truthy)
