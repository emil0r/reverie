(ns reveriedev.dev
  (:require [reveriedev.init :as init]
            [reverie.server :as server]))


(init/init)
(server/init)
(def app (server/server-handler {}))
