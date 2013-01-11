(ns reverie.test.scenarios.scenario1
  (:require [reverie.core :as rev]
            [reverie.schema.datomic :as _] ;; for reloads in midje
            )
  (:use midje.sweet
        [datomic.api :only [q db] :as d]
        [reverie.test.core :only [setup]])
  (:import reverie.core.ObjectDatomic))


(fact
 "defining template"
 (let [{:keys [database connection]} (setup)]
   ) => nil)
