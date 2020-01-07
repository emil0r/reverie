(ns reverie.page.api
  (:require [clojure.core.match :refer [match]]
            [cheshire.core :as json]
            [reverie.auth :refer [with-access]]
            [reverie.http.response :as response]
            [reverie.http.route :as route]
            [reverie.page.util :refer [handle-response]]
            [reverie.util :as util]
            [ring.swagger.swagger-ui :as ui]))


(defn get-schema [request page payload _]
  {:status 200
   :body (json/generate-string (get-in page [:options :openapi]))
   :headers {"Content-Type" "application/json"}})


(defn get-docs [request {{docs-handler :docs-handler openapi :openapi} :options
                         route :route :as page} payload _]
  (let [resp (docs-handler (assoc request :context (str (:path route) (get openapi :docs-path "/docs"))))]
    (if (and (:body resp)
             (not (string? (:body resp))))
      (update-in resp [:body] slurp)
      resp)))

(defn get-payload [request page]
  (if-let [body (:body request)]
    (json/parse-string (slurp body) true)
    nil))

(defn get-response [resp]
  (if (and (map? resp)
           (contains? resp :status)
           (contains? resp :body)
           (contains? resp :headers))
    resp
    (let [[status data] (if (vector? resp)
                          resp
                          [200 resp])]
      {:status status
       :body (json/generate-string data)
       :headers {"Content-Type" "application/json"}})))


(defn render-fn [{:keys [route options routes] :as this} {:keys [request-method] :as request}]
  (with-access
    (get-in request [:reverie :user])
    (:required-roles options)
    (handle-response
     options
     (if-let [page-route (first (filter #(route/match? % request) routes))]
       (let [{:keys [request method]} (route/match? page-route request)]
         (if (and request method)
           (get-response (method request this (get-payload request this) (:params request)))
           (response/raise 404)))
       (response/raise 404)))))
