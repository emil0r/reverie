(ns reverie.page.api
  (:require [clojure.core.match :refer [match]]
            [cheshire.core :as json]
            [reverie.auth :refer [with-access]]
            [reverie.http.response :as response]
            [reverie.http.route :as route]
            [reverie.page.util :refer [handle-response]]
            [reverie.util :as util]
            [ring.swagger.swagger-ui :as ui]))


(defn get-schema [request page _]
  {:status 200
   :body (json/generate-string (get-in page [:options :openapi]))
   :headers {"Content-Type" "application/json"}})

(defn get-docs [request {{docs-handler :docs-handler openapi :openapi} :options
                         route :route :as page} _]
  (println docs-handler)
  (let [resp (docs-handler (assoc request :context (str (:path route) (get openapi :docs-path "/docs"))))]
    (if (and (:body resp)
             (not (string? (:body resp))))
      (update-in resp [:body] slurp)
      resp)))

(def catch (atom {}))


(defn render-fn [{:keys [route options routes] :as this} {:keys [request-method] :as request}]
  (let [#_#_request (merge request
                       {:shortened-uri (util/shorten-uri
                                        (:uri request) (:path route))})]
    (with-access
      (get-in request [:reverie :user])
      (:required-roles options)
      (handle-response
       options
       (if-let [page-route (first (filter #(route/match? % request) routes))]
         (let [{:keys [request method]} (route/match? page-route request)]
           (if (and request method)
             (let [resp (method request this (:params request))
                   middleware-handler-page-route (get-in page-route [:options :middleware])
                   middleware-handler (get options :middleware)
                   final-resp (match [ ;; raw response
                                      (and (map? resp)
                                           (contains? resp :status)
                                           (contains? resp :body)
                                           (contains? resp :headers))]
                                     
                                     ;; default
                                     [_] resp)]
               (match [(nil? middleware-handler) (nil? middleware-handler-page-route)]
                      [false false]
                      (middleware-handler
                       (assoc-in
                        request [:reverie :response]
                        (middleware-handler-page-route
                         (assoc-in request [:reverie :response] final-resp))))

                      [false true]
                      (middleware-handler (assoc-in request [:reverie :response] final-resp))

                      [true false]
                      (middleware-handler-page-route (assoc-in request [:reverie :response] final-resp))

                      [_ _]
                      final-resp))))
         (response/raise 404))))))
