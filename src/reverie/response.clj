(ns reverie.response)

(def ^:private responses
  (atom
   {:401 {:body "You are not authorized to do this!" :headers nil}
    :403 {:body "Forbidden" :headers nil}
    :404 {:body "Page Not Found" :headers nil}
    :500 {:body "Internal Server Error" :headers nil}}))

(defn update-response! [{:keys [code body headers]}]
  (swap! responses assoc code {:body body :headers headers}))

(defn response-301 [uri]
  {:status 301 :headers {"Location " uri} :body ""})

(defn response-302 [uri]
  {:status 302 :headers {"Location " uri} :body ""})

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
