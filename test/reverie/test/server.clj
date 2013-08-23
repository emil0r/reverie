(ns reverie.test.server
  (:require [reverie.core :as rev]
            [reverie.server :as server])
  (:use midje.sweet
        ring.mock.request))


(defn wrap-count [handler]
  (fn [request]
    (let [response (handler request)
          level (get-in response [:headers "level"] 1)]
      (assoc-in response [:headers "level"] (+ level 1)))))

(fact
 "server-handler"
 (get-in ((server/server-handler {:handlers [[wrap-count] [wrap-count]]})
          {:level 1 :uri "/server-handler"}) [:headers "level"]) => 3)

(fact
 "start server, stop server, restart server"
 (let [s (server/start {:port 8888 :handlers []})]
   (server/restart s)
   (server/stop s)
   s) => truthy)
