(ns reverie.responses)

(def response-403
  {:status 403 :body "Forbidden"})

(def response-500
  {:status 500 :body "Internal Server Error"})

(defn response-302 [uri]
  {:status 302 :body (str "Location: " uri)})

(defn response-301 [uri]
  {:status 301 :body (str "Location: " uri)})
