(ns reverie.modules.default
  (:require [reverie.system :as sys]))

(defn list-entities [request module params]
  {})


(swap! sys/storage assoc :module-default-routes
       [["/" {:get list-entities}]])
