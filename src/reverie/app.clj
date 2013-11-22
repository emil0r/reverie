(ns reverie.app
  (:require [clojure.string :as s]
            [clout.core :as clout]
            [korma.core :as k]
            [reverie.response :as r]
            [reverie.settings :as settings]
            [reverie.util :as util])
  (:use [reverie.atoms :exclude [objects]]
        reverie.entity
        [reverie.helpers :only [with-template]]))

(defn- convert-areas [area-mappings areas]
  (reduce (fn [out k]
            (if (nil? k)
              out
              (assoc out k (get areas (k area-mappings)))))
          {}
          (keys area-mappings)))

(defn render
  "Renders the app"
  [{:keys [reverie] :as request}]
  (if-let [app (@apps (:app reverie))]
    (let [p (:page reverie)
          request (util/shorten-uri request (:uri p))
          app-options (:options app)
          [_ route options f] (->> app
                                   :fns
                                   (filter #(let [[method route _ _] %]
                                              (and
                                               (= (:request-method request) method)
                                               (clout/route-matches route
                                                                    request))))
                                   first)
          request (assoc-in request [:reverie :app/path] (:app/path options))]
      (if (nil? f)
        (r/response-404)
        (let [resp (if (= :get (:request-method request))
                     (util/middleware-wrap
                      (util/middleware-merge app-options options)
                      f request (clout/route-matches route request))          
                     (util/middleware-wrap
                      (util/middleware-merge app-options options)
                      f request (clout/route-matches route request) (:params request)))]
          (if (settings/option-true? :app (:app p) [:app/type] :template)
            (assoc resp :body
                   (with-template
                     (keyword (:template p))
                     request
                     (convert-areas
                      (:app_template_bindings p)
                      (:body resp))))
            resp))))))
