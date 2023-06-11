(ns reverie.http.route
  (:require [clojure.string :as str]
            [clout.core :as clout]
            [reverie.cast :as cast]
            [reverie.system :as sys]
            [reverie.util :as util :refer [regex?]]
            [schema.core :as s]))

(defprotocol IRouting
  (match? [component request])
  (get-route [component]))

(s/defrecord Route [parent-path path options compiled permissions match methods
                    parameters]
  IRouting
  (match? [this request]
    (let [temp-request (if parent-path
                         (assoc request :uri (util/shorten-uri (:uri request) parent-path))
                         request)]
      (if-let [matched (clout/route-matches compiled temp-request)]
        (let [method (if methods
                       (or (get methods (:request-method request))
                           (:any methods)))]
          {:matched matched
           :method method}))))
  (get-route [this] this))


(defn- get-data [[path data]]
  (let [path (if (str/blank? path)
               "/"
               path)
        options (dissoc data :get :post :options :any :put :delete :permissions)]
    (assoc
     (select-keys data [:parameters :match :permissions])
     :path path
     :methods (select-keys data [:get :post :options :any :put :delete])
     :options options
     :compiled (if (:match data)
                 (clout/route-compile path (:match data))
                 (clout/route-compile path)))))

(defn route
  ([-route]
   (route nil -route))
  ([parent-path route]
   (let [settings (get-data route)]
     (assert (not (str/blank? (:path settings))) "Path must be a non-empty string containing a URI")
     (map->Route (assoc settings :parent-path parent-path)))))
