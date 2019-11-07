(ns reverie.test.validation
  (:require [reverie.admin.validation :as v]
            [reverie.module :as m]
            reverie.modules.auth
            [reverie.system :as sys]
            [midje.sweet :refer :all]
            [vlad.core :as vlad]))


(fact
 "validate"
 (->> (v/validate
       (-> @sys/storage :modules :auth :module (m/get-entity "user"))
       {})
      (filter (fn [{:keys [selector]}]
                (some #(= selector %) [[:username] [:email]]))))
 => [{:selector [:username], :type :vlad.core/present} {:selector [:email], :type :vlad.core/present}])
