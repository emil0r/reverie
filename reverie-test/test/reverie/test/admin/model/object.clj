(ns reverie.test.admin.model.object
  (:require [com.stuartsierra.component :as component]
            [reverie.admin.http-responses :as http]
            [reverie.admin.model.object :as object]
            [reverie.test.database.sql-helpers :refer [get-admin-user
                                                       seed!
                                                       start-db
                                                       stop-db]]
            [midje.sweet :refer :all]))

(fact
 "admin object"
 (let [db (start-db)
       page-id 1
       user (get-admin-user db)]
   (try
     (fact "get-objects"
           (fact "page-id = 1"
                 (->> (object/get-objects db page-id)
                      (map :page/id)
                      (into #{}))
                 => #{1})
           (fact "same amount of object ids as versions"
                 (let [objects (object/get-objects db page-id)
                       data (map (juxt :object/id :object/version) objects)
                       count-ids (count (into #{} (map first data)))
                       count-versions (count (into #{} (map second data)))]
                   (= count-ids count-versions))
                 => true))
     (fact "add-object"
           (object/add-object db user {:page-serial 1
                                       :area :a
                                       :object-name :reverie/text
                                       :properties {:text "My test"}})
           => http/success)
     (finally
       (stop-db db)))))
