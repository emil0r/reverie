(ns reveriedev.dev
  (:require [reveriedev.init :as init]
            [reverie.server :as server]))


(init/init)
(def app (server/server-handler {}))
