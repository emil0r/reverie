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

(s/defrecord Route [parent-path path options compiled roles matching casting methods]
  IRouting
  (match? [this request]
    (let [temp-request (if parent-path
                         (assoc request :uri (util/shorten-uri (:uri request) parent-path))
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

(defn- casting? [x]
  (= (type x) java.lang.Class))

(defn- method? [x]
  (fn? x))

(defn- get-data [route]
  (let [raw-data (reduce (fn [out arg]
                           (cond
                             (string? arg)
                             (assoc out :path (if (str/blank? arg)
                                                "/"
                                                arg))

                             (set? arg)
                             (assoc out :roles arg)

                             (and (map? arg) (some regex? (vals arg)))
                             (assoc out :matching arg)

                             (and (map? arg) (true? (:meta (meta arg))))
                             (assoc out :options arg)

                             (and (map? arg) (some casting? (vals arg)))
                             (assoc out :casting arg)

                             (and (map? arg) (some method? (vals arg)))
                             (assoc out :methods arg)

                             :else out))
                         {:path nil
                          :options nil
                          :roles nil
                          :matching nil
                          :casting nil
                          :methods nil} route)]
    (assoc raw-data :compiled
           (if (:matching raw-data)
             (clout/route-compile (:path raw-data) (:matching raw-data))
             (clout/route-compile (:path raw-data))))))

(defn route
  ([-route]
   (route nil -route))
  ([parent-path route]
   (let [settings (get-data route)]
     (assert (not (str/blank? (:path settings))) "Path must be a non-empty string containing a URI")
     (map->Route (assoc settings :parent-path parent-path)))))
