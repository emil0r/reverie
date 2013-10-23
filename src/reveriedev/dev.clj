(ns reveriedev.dev
  (:require [reveriedev.init :as init]
            [reverie.atoms :as atoms]
            [reverie.server :as server]))


(defn init []
  (swap! atoms/settings assoc :server-mode :debug))

(init/init)
(server/init)
(def app (server/server-handler {}))
