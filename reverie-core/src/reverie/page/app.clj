(ns reverie.page.app
  (:require [clojure.core.match :refer [match]]
            [reverie.auth :refer [with-access]]
            [reverie.http.response :as response]
            [reverie.http.route :as route]
            [reverie.page.util :refer [handle-response]]
            [reverie.render :as render]
            [reverie.system :as sys]
            [reverie.util :as util]
            [taoensso.timbre :as log]))


(defn render-fn [{:keys [route options app-routes properties template] :as this} request]
  (let [request (merge request
                       {:shortened-uri (util/shorten-uri
                                        (:uri request) (:path route))})]
    (handle-response
     options
     (if-let [app-route (first (filter #(route/match? % request) app-routes))]
       (let [{:keys [request method]} (route/match? app-route request)
             renderer (sys/renderer (:renderer options))
             resp (method request this properties (:params request))
             t (or (:template properties) template)]
         (match [(not (nil? renderer)) (not (nil? (:methods-or-routes renderer))) (map? resp) (not (nil? t))]
                ;; ~renderer, _ , map, template
                [false _ true true] (render/render t request (assoc this :rendered resp))

                ;; renderer, routes, map, template
                [true true true true] (let [out (render/render renderer (:request-method request)
                                                               {:data resp
                                                                ::render/type :page/routes
                                                                :meta {:route-name (get-in app-route [:options :name])}})]
                                        (render/render t request (assoc this :rendered out)))

                ;; renderer, ~routes, map, template
                [true false true true] (let [out (render/render renderer (:request-method request)
                                                                {:data resp
                                                                 ::render/type :page/no-routes
                                                                 :meta {:route-name (get-in app-route [:options :name])}})]
                                         (render/render t request (assoc this :rendered out)))

                ;; _, _, ~map, template
                ;; invalid combiation
                ;; when a template is being used there has to be a map of some sort
                [_ _ false true] (do (log/error {:what ::AppPage
                                                 :message "Invalid combitation rendering AppPage. Tried to render a template without a corresponding map"
                                                 :template t
                                                 :route route
                                                 :app-route app-route
                                                 :response resp})
                                     (response/raise 500))
                ;; default
                [_ _ _ _] resp))))))

