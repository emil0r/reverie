(ns reverie.responses)

(def response-403
  {:status 403 :body "Forbidden"})

(def response-404
  {:status 404 :body "Page Not Found"})

(def response-500
  {:status 500 :body "Internal Server Error"})

(defn response-302 [uri]
  {:status 302 :headers {"Location " uri}})

(defn response-301 [uri]
  {:status 301 :headers {"Location " uri}})

(defn response-401 [& [response]]
  {:status 401 :body (or response "You are not authorized to do this!")})
