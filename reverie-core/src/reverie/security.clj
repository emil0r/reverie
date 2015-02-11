(ns reverie.security
  (:require [clojure.set :as set]
            [slingshot.slingshot :refer [throw+]]))


(defmacro with-access [user required-roles & body]
  `(if (or (nil? ~required-roles)
           (not (empty? (set/intersection
                         (:roles ~user) ~required-roles))))
     ~@body
     (throw+ {:type ::not-allowed})))
