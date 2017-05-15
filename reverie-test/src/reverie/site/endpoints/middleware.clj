(ns reverie.site.endpoints.middleware
  (:require [reverie.core :refer [defpage]]
            [ring.middleware.json :refer [wrap-json-response]]))



(defn middleware-test [request page params]
  {:body {:test true}
   :status 200})

(defpage "/middleware"
  {}
  [["/test" ^:meta {:middleware [[wrap-json-response]]} {:get middleware-test}]])
