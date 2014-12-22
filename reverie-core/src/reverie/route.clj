(ns reverie.route
  (:require [clout.core :as clout]
            [reverie.cast :as cast]))

(defprotocol RoutingProtocol
  (match? [component request])
  (get-route [component]))


(defrecord Route [path compiled matching casting methods]
  RoutingProtocol
  (match? [this request]
    (if-let [matched (clout/route-matches compiled request)]
      (let [method (or (get methods (:request-method request))
                       (:any methods))]
        {:request (if casting
                    (assoc request
                      :params
                      (reduce (fn [params [key cast-to]]
                                (if (get params key)
                                  (assoc params key (cast/cast cast-to (get params key)))
                                  params))
                              (merge (:params request) matched) casting))
                    request)
         :matched matched
         :method method})))
  (get-route [this] this))


(defn route [route]
  (let [[path matching? casting? methods] route
        [matching casting methods] (case [(nil? casting?) (nil? methods)]
                                     [true true] [nil nil matching?]
                                     [false true] [matching? nil casting?]
                                     [false false] [matching? casting? methods])]
    (Route. path (if matching
                   (clout/route-compile path matching)
                   (clout/route-compile path))
            matching casting methods)))
