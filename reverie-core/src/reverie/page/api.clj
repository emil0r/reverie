(ns reverie.page.api
  (:require [clojure.core.match :refer [match]]
            [cheshire.core :as json]
            [reverie.auth :refer [with-access]]
            [reverie.http.response :as response]
            [reverie.http.route :as route]
            [reverie.page.util :refer [handle-response]]
            [reverie.util :as util]
            [ring.swagger.swagger-ui :as ui]))


(defn build-api-routes [out
                        parent-route
                        extras
                        [[route {:keys [methods] :as meta-properties} & child-routes]
                         & routes]]
  (let [new-route (str parent-route route)
        new-methods (->> methods ; take the methods in the current route
                         (map (fn [[k {:keys [handler]}]] [k handler])) ; take out k and handler
                         (into {})) ; create a new map with all http methods and corresponding handlers
        openapi-data (select-keys meta-properties [:parameters :tags])
        ;; gather up info for the openapi spec
        openapi-info {new-route (into {} (map (fn [[k properties]]
                                                [k (util/deep-merge
                                                    (:openapi-data extras)
                                                    openapi-data
                                                    (dissoc properties :handler))])
                                              methods))}
        new-out (-> out
                    (update-in [:routes] conj [new-route (get-in openapi-data [:parameters :path]) new-methods])
                    (update-in [:openapi :paths] merge openapi-info))]
    (cond
      (not (empty? child-routes))
      (recur new-out
             new-route
             (update-in extras [:openapi-data] merge openapi-data)
             child-routes)
      
      (not (empty? routes))
      (recur new-out new-route extras routes)
      
      :else
      new-out)))


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
    (let [[status data] resp]
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
