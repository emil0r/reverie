(ns reverie.test.server
  (:require [reverie.core :as rev]
            [reverie.server :as server])
  (:use midje.sweet
        [reverie.test.util :only [wrap-count]]
        ring.mock.request))




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
