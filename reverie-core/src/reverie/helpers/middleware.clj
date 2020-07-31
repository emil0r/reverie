(ns reverie.helpers.middleware
  (:require [clojure.core.match :refer [match]]
            [clojure.walk :refer [postwalk]]
            [reverie.page.middleware :refer [wrap-page-render]]))


(defn create-handler [handlers routes]
  (reduce (fn [current new]
            (if (nil? new)
              current
              (let [[new & args] new]
                (apply new current args))))
          routes
          handlers))

(defn merge-handlers [handlers-page handlers-route]
  (match [(not (empty? handlers-page)) (not (empty? handlers-route))]
         [true true] (into handlers-page handlers-route)
         [true false] handlers-page
         [false true] handlers-route
         [_ _] nil))



(defn- massage-routes [page default-handler]
  (fn [{:keys [options] :as route}]
    (if (and (map? options)
             (contains? options :middleware))
      (assoc options :middleware (create-handler (merge-handlers default-handler (:middleware options))
                                                 (wrap-page-render page))))))

(defn add-page-middleware [page]
  (let [handlers-page (get-in page [:options :middleware])
        default-handler (if handlers-page
                          (create-handler handlers-page (wrap-page-render page)))
        routes (:routes page)]
    (assoc page :routes (postwalk (fn [?route]
                                    (if (map? ?route)
                                      (let [options (:options ?route)]
                                        (cond
                                          (contains? options :middleware)
                                          (assoc-in ?route [:options :middleware] (create-handler (merge-handlers handlers-page (:middleware options))
                                                                                                  (wrap-page-render page)))
                                          (some? default-handler)
                                          (assoc-in ?route [:options :middleware] default-handler)
                                          
                                          :else
                                          ?route))
                                      ?route))
                                  routes))))
