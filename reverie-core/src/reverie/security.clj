(ns reverie.security
  (:require [clojure.set :as set]
            [slingshot.slingshot :refer [throw+]]))


(defmacro with-access [user required-roles & body]
  `(if (or (contains? (:roles ~user) :admin)
           (nil? ~required-roles)
           (not (empty? (set/intersection
                         (:roles ~user) (into #{} (vals ~required-roles))))))
     ~@body
     (throw+ {:type ::not-allowed})))


(defmacro with-authorize [user action required-roles & body]
  `(if (or (contains? (:roles ~user) :admin)
           (nil? ~required-roles)
           (not (empty? (set/intersection
                         (:roles ~user) (get ~required-roles ~action)))))
     ~@body
     (throw+ {:type ::not-allowed})))


(defprotocol IAuthorize
  (authorize? [what user database action])
  (add-authorization! [what database role action])
  (remove-authorization! [what database role action]))
