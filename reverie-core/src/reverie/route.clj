(ns reverie.route
  (:require [clojure.string :as str]
            [clout.core :as clout]
            [reverie.cast :as cast]))

(defprotocol IRouting
  (match? [component request])
  (get-route [component]))


(defrecord Route [path compiled matching casting methods]
  IRouting
  (match? [this request]
    (let [temp-request (if (:shortened-uri request)
                         (assoc request :uri (:shortened-uri request))
                         request)]
      (if-let [matched (clout/route-matches compiled temp-request)]
        (let [method (if methods
                       (or (get methods (:request-method request))
                           (:any methods)))]
          {:request (if casting
                      (assoc request
                        :params
                        (reduce (fn [params [key cast-to]]
                                  (if (get params key)
                                    (assoc params key (cast/cast cast-to (get params key)))
                                    params))
                                (merge (:params request) matched) casting))
                      (assoc request :params (merge (:params request) matched)))
           :matched matched
           :method method}))))
  (get-route [this] this))


(defn route [route]
  (let [[path matching? casting? methods] route
        [matching casting methods] (case [(nil? casting?) (nil? methods)]
                                     [true true] [nil nil matching?]
                                     [false true] [matching? nil casting?]
                                     [false false] [matching? casting? methods])]
    (let [path (if (str/blank? path) "/" path)]
     (Route. path (if matching
                    (clout/route-compile path matching)
                    (clout/route-compile path ))
             matching casting methods))))
