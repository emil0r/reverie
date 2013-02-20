(ns reverie.test.schema.datomic.page
  (:require [reverie.core :as rev]
            [reverie.schema.datomic :as _] ;; for reloads in midje
            )
  (:use midje.sweet
        [datomic.api :only [q db] :as d]
        [reverie.test.core :only [setup]])
  (:import reverie.core.ObjectDatomic reverie.core.ReverieDataDatomic))


(defn- init-data [command data]
  (merge {:command command
          :parent nil
          :name "my test page"
          :uri "my-test-page"
          :template :main
          :rights :?} data))

(fact
 "add page"
 (let [{:keys [connection]} (setup)
       request {}
       data (init-data :page-new nil)
       rdata (ReverieDataDatomic. connection request data)]
   (-> rdata rev/page-new! :db/id pos?))
 => true)

(fact
 "get page"
 (let [{:keys [connection]} (setup)
       request {}
       data (init-data :page-new nil)
       rdata (ReverieDataDatomic. connection request data)
       new-page-id (-> rdata rev/page-new! :db/id)]
   (= new-page-id (:db/id (rev/page-get rdata new-page-id))))
 => true)

(fact
 "update page, delete page & restore page"
 (let [{:keys [connection]} (setup)
       request {}
       data (init-data :page-new nil)
       rdata (ReverieDataDatomic. connection request data)
       page (rev/page-new! rdata)
       ]
   )
 => {:update true
     :delete true
     :restore true})



;; (defn tempus [rdata]
;;   (rev/area a))
;; (tempus (ReverieDataDatomic. nil {} nil {:mode :public}))
