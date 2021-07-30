(ns reverie.specs.route
  (:require [clojure.spec.alpha :as spec]
            [reverie.util :refer [regex?]]))

(defn regex-or-symbol? [x]
  (or (regex? x)
      (symbol? x)))

(spec/def :reverie.http.route/path string?)
(spec/def :reverie.http.route/casting (spec/and map?
                                                (fn [arg]
                                                  (some #(or
                                                          (symbol? %)
                                                          (instance? java.lang.Class %)) (vals arg)))))
(spec/def :reverie.http.route/matching (spec/and map?
                                                 #(some regex-or-symbol? (vals %))))
(spec/def :reverie.http.route/meta (spec/and map?
                                             #(true? (:meta (meta %)))))
(spec/def :reverie.http.route/roles (spec/and set?
                                              #(every? keyword? %)))

(spec/def :reverie.http.route/http-methods
  (spec/map-of #{:any :get :post :put :delete :head :options}
               (spec/or :fn fn?
                        :symbol symbol?)))

(spec/def :reverie.http.route/route (spec/cat
                                     :path :reverie.http.route/path
                                     :meta (spec/? :reverie.http.route/meta)
                                     :matching (spec/? :reverie.http.route/matching)
                                     :casting (spec/? :reverie.http.route/casting)
                                     :roles (spec/? :reverie.http.route/roles)
                                     :http-methods :reverie.http.route/http-methods))

(spec/def :reverie.http.route/routes (spec/coll-of :reverie.http.route/route))
