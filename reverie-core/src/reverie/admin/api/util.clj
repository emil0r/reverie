(ns reverie.admin.api.util
  (:require [cheshire.core :as json]))

(defn json-response [body]
  (let [body (cond
              (boolean? body) {:success body}
              :else body)]
    {:status 200
     :headers {"Content-Type" "json/application"}
     :body (json/generate-string body)}))
