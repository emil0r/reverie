(ns reveriecms.dev
  (:require [reveriecms.init :as init]
            [reverie.server :as server]))


(init/init)
(def app (server/generate-handler {}))
