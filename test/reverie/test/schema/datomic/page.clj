(ns reverie.test.schema.datomic.page
  (:require [reverie.core :as rev]
            [reverie.schema.datomic :as _] ;; for reloads in midje
            )
  (:use midje.sweet
        [datomic.api :only [q db] :as d]
        [reverie.test.core :only [setup]])
  (:import reverie.core.ObjectDatomic reverie.core.ReverieDataDatomic))



(fact
 "add page"
 (let [{:keys [database connection]} (setup)
       request {:command :page-new
                :data {:parent nil
                       :name "my test page"
                       :uri "my-test-page"
                       :template :main
                       :rights :?}}
       page-id nil
       attributes {}
       rdata (ReverieDataDatomic. connection request page-id attributes)]
   (-> rdata rev/page-new :db/id pos?))
 => true)



;; (defn tempus [rdata]
;;   (rev/area a))
;; (tempus (ReverieDataDatomic. nil {} nil {:mode :public}))
