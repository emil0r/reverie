(ns reverie.app
  (:require [clojure.string :as s]
            [clout.core :as clout]
            [korma.core :as k]
            [reverie.util :as util])
  (:use [reverie.core :exclude [objects]]
        reverie.entity))


(defn render
  "Renders the app"
  [{:keys [page] :as request}]
  (if-let [app (@apps (:app page))]
      (let [request (util/shorten-uri request (:uri page))
            [_ route _ func] (->> app
                                  :fns
                                  (filter #(let [[method route _ _] %]
                                             (and
                                              (= (:request-method request) method)
                                              (clout/route-matches route request))))
                                  first)]
        (if (nil? func)
          {:status 404 :body "404, page not found"}
          (func request (clout/route-matches route request))))))
