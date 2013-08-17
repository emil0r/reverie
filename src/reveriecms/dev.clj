(ns reveriecms.dev
  (:require [reverie.server :as server]))


(def app (server/generate-handler {}))
