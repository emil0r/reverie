(ns reverie.app
  (:require [clojure.string :as s]
            [clout.core :as clout]
            [korma.core :as k]
            [reverie.responses :as r]
            [reverie.util :as util])
  (:use [reverie.atoms :exclude [objects]]
        reverie.entity))


(defn render
  "Renders the app"
  [{:keys [page] :as request}]
  (if-let [app (@apps (keyword (:app page)))]
    (let [request (util/shorten-uri request (:uri page))
          app-options (:options app)
          [_ route options f] (->> app
                                   :fns
                                   (filter #(let [[method route _ _] %]
                                              (and
                                               (= (:request-method request) method)
                                               (clout/route-matches route request))))
                                   first)]
      (if (nil? f)
        r/response-404
        (if (= :get (:request-method request))
          (util/middleware-wrap
           (util/middleware-merge app-options options)
           f request (clout/route-matches route request))
          
          (util/middleware-wrap
           (util/middleware-merge app-options options)
           f request (clout/route-matches route request) (:params request)))))))
