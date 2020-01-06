(ns reverie.page.raw-page
  (:require [clojure.core.match :refer [match]]
            [reverie.auth :refer [with-access]]
            [reverie.http.response :as response]
            [reverie.http.route :as route]
            [reverie.page.util :refer [handle-response]]
            [reverie.render :as render]
            [reverie.system :as sys]
            [reverie.util :as util]))


(defn render-fn [{:keys [route routes options] :as this} {:keys [request-method] :as request}]
  (let [request (merge request
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
             (let [renderer (sys/renderer (:renderer options))
                   resp (method request this (:params request))
                   t (sys/template (:template options))
                   middleware-handler-page-route (get-in page-route [:options :middleware])
                   middleware-handler (get options :middleware)
                   final-resp (match [;; raw response
                                      (and (map? resp)
                                           (contains? resp :status)
                                           (contains? resp :body)
                                           (contains? resp :headers))

                                      ;; renderer
                                      (not (nil? renderer))

                                      ;; routes
                                      (not (nil? (:methods-or-routes renderer)))

                                      ;; map
                                      (map? resp)

                                      ;; template
                                      (not (nil? t))]

                                     ;; raw response
                                     [true _ _ _ _] resp

                                     ;; ~renderer, _ , map, template
                                     [_ false _ true true] (render/render t request (assoc this :rendered resp))

                                     ;; renderer, routes, map, template
                                     [_ true true true true] (let [out (render/render renderer (:request-method request)
                                                                                      {:data resp
                                                                                       ::render/type :page/routes
                                                                                       :meta {:route-name (get-in page-route [:options :name])}})]
                                                               (render/render t request (assoc this :rendered out)))

                                     ;; renderer, ~routes, map, template
                                     [_ true false true true] (let [out (render/render renderer (:request-method request)
                                                                                       {:data resp
                                                                                        ::render/type :page/no-routes
                                                                                        :meta {:route-name (get-in page-route [:options :name])}})]
                                                                (render/render t request (assoc this :rendered out)))

                                     ;; renderer, ~routes, ~map, ~template
                                     [_ true false false false] (render/render renderer (:request-method request)
                                                                               {:data resp
                                                                                ::render/type :page/no-routes
                                                                                :meta {:route-name (get-in page-route [:options :name])}})

                                     ;; default
                                     [_ _ _ _ _] resp)]
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

