(ns reverie.test.schema.datomic.page
  (:require [reverie.core :as rev]
            [reverie.schema.datomic :as _] ;; for reloads in midje
            )
  (:use midje.sweet
        [datomic.api :only [q db] :as d]
        [reverie.test.core :only [setup]])
  (:import reverie.core.ObjectDatomic reverie.core.ReverieDataDatomic))


(defn tempus [rdata]
  (rev/area a))
(tempus (ReverieDataDatomic. nil {} nil {:mode :public}))
