(ns reverie.page.rawpage
  (:require [clojure.core.match :refer [match]]
            [reverie.auth :refer [with-access]]
            [reverie.http.response :as response]
            [reverie.http.route :as route]
            [reverie.page.util :refer [find-route handle-response]]
            [reverie.page.middleware :refer [wrap-page-middleware]]
            [reverie.render :as render]
            [reverie.system :as sys]
            [reverie.util :as util]))


(defn render-page [page {:keys [request-method] :as request}]
  (let [{:keys [options routes route method]} page]
      (handle-response
       options
       (if-let [page-route (find-route request routes)]
         (let [{:keys [request method]} (route/match? page-route request)]
           (if (and request method)
             (let [renderer (sys/renderer (:renderer options))
                   ;; middleware-page (:middleware options)
                   ;; middleware-route (get-in page-route [:options :middleware])
                   resp (method request page (:params request))
                   t (sys/template (:template options))]
               (match [;; raw response
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
                      [_ false _ true true] (render/render t request (assoc page :rendered resp))

                      ;; renderer, routes, map, template
                      [_ true true true true] (let [out (render/render renderer (:request-method request)
                                                                       {:data resp
                                                                        ::render/type :page/routes
                                                                        :meta {:route-name (get-in page-route [:options :name])}})]
                                                (render/render t request (assoc page :rendered out)))

                      ;; renderer, ~routes, map, template
                      [_ true false true true] (let [out (render/render renderer (:request-method request)
                                                                        {:data resp
                                                                         ::render/type :page/no-routes
                                                                         :meta {:route-name (get-in page-route [:options :name])}})]
                                                 (render/render t request (assoc page :rendered out)))

                      ;; renderer, ~routes, ~map, ~template
                      [_ true false false false] (render/render renderer (:request-method request)
                                                                {:data resp
                                                                 ::render/type :page/no-routes
                                                                 :meta {:route-name (get-in page-route [:options :name])}})

                      ;; default
                      [_ _ _ _ _] resp))))
         (response/raise 404)))))
