(ns reverie.response)

(def ^:private responses
  (atom
   {:401 {:body "You are not authorized to do this!" :headers {"Content-Type" "text/html; charset=utf-8"}}
    :403 {:body "Forbidden" :headers {"Content-Type" "text/html; charset=utf-8"}}
    :404 {:body "Page Not Found" :headers {"Content-Type" "text/html; charset=utf-8"}}
    :500 {:body "Internal Server Error" :headers {"Content-Type" "text/html; charset=utf-8"}}}))

(defn update-response! [code {:keys [body headers]}]
  (if body
    (swap! responses assoc-in [code :body] body))
  (if headers
    (swap! responses assoc-in [code :headers] headers)))

(defn response-301 [url]
  {:status 301 :headers {"Location" url} :body ""})

(defn response-302 [url]
  {:status 302 :headers {"Location" url} :body ""})

(defn response-401
  ([]
     {:status 401 :body (-> @responses :401 :body) :headers (-> @responses :401 :headers)})
  ([response]
     {:status 401 :body (or response (-> @responses :401 :body))
      :headers (-> @responses :401 :headers)}))

(defn response-403
  ([]
     {:status 403 :body (-> @responses :403 :body) :headers (-> @responses :403 :headers)})
  ([response]
     {:status 403 :body (or response (-> @responses :403 :body))
      :headers (-> @responses :403 :headers)}))

(defn response-404
  ([]
     {:status 404 :body (-> @responses :404 :body) :headers (-> @responses :404 :headers)})
  ([response]
     {:status 404 :body (or response (-> @responses :404 :body))
      :headers (-> @responses :404 :headers)}))

(defn response-500
  ([]
     {:status 500 :body (-> @responses :500 :body) :headers (-> @responses :500 :headers)})
  ([response]
     {:status 500 :body (or response (-> @responses :500 :body))
      :headers (-> @responses :500 :headers)}))
