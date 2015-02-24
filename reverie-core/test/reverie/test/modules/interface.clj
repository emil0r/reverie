(ns reverie.test.modules.interface
  (:require [com.stuartsierra.component :as component]
            [reverie.admin.looknfeel.form :as looknfeel]
            reverie.auth
            [reverie.database :as database]
            [reverie.module :as module]
            [reverie.module.entity :as entity]
            reverie.modules.auth
            [reverie.modules.sql :as msql]
            [reverie.system :as sys]
            [reverie.test.database.sql-helpers :refer [seed! get-db]]
            [midje.sweet :refer :all]))



(fact
 "module data manipulation"
 (let [db (component/start (get-db))
       mod (assoc (-> @sys/storage :modules :auth :module)
             :database db)]
   (try
     (seed!)
     (let [user-ent (module/get-entity mod "user")
           group-ent (module/get-entity mod "group")]
       ;; add two groups
       (module/add-data mod group-ent {:name "Foobar"})
       (module/add-data mod group-ent {:name "Baz"})

       ;; change first user (admin)
       (module/save-data mod user-ent 1 {:full_name "Admin Adminsson"
                                         :spoken_name "Mr Admin"
                                         :email "admin@reveriecms.org"
                                         :roles [1 2 3]
                                         :groups [1 2 3]})
       (module/add-data mod user-ent {:username "user1" :password "foo"
                                      :spoken_name "Mr User1"
                                      :full_name "User1 Smith"
                                      :email "user1@smith.org"
                                      :roles ["2"]
                                      :groups "3"})
       (module/add-data mod user-ent {:username "user2" :password "bar"
                                      :spoken_name "Mr User2"
                                      :full_name "User2 Smith"
                                      :email "user2@smith.org"
                                      :roles [3]
                                      :groups [3]})

       (module/delete-data mod user-ent 3 true)
       (select-keys (:form-data (module/get-data mod user-ent 2))
                    [:id :username :roles :groups])
       => {:id 2 :username "user1" :roles #{2} :groups #{3}})
     (catch Exception e
       (println e)))
   (component/stop db)))


(fact
 "module data get"
 (let [db (component/start (get-db))
       mod (assoc (-> @sys/storage :modules :auth :module)
             :database db)]
   (try
     (seed!)
     (let [user-ent (module/get-entity mod "user")
           group-ent (module/get-entity mod "group")]
       (select-keys (:form-data (module/get-data mod user-ent {} 1))
                    [:id :username :roles :groups])
       => {:id 1 :username "admin" :roles #{1} :groups #{1}})
     (catch Exception e
       (println e)))
   (component/stop db)))


(fact
 "casting"
 (let [mod (-> @sys/storage :modules :auth :module)]
   (let [user-ent (module/get-entity mod "user")]
     (msql/cast-data user-ent {:username "admin",
                               :email "admin@reveriecms.org",
                               :spoken_name "Mr Admin",
                               :full_name "Admin Adminsson",
                               :active_p "true",
                               :roles "1"}))
   => {:username "admin",
       :email "admin@reveriecms.org",
       :spoken_name "Mr Admin",
       :full_name "Admin Adminsson",
       :active_p true,
       :roles [1]}))


(fact
 "looknfeel/row"
 (let [mod (-> @sys/storage :modules :auth :module)]
   (let [user-ent (module/get-entity mod "user")]
     (fact ":default"
           (looknfeel/row user-ent :username {})
           => [:div.form-row
               []
               [:label {:for "username"} "Username"]
               [:input {:id "username", :max 255, :name "username", :type "text", :value nil}]
               [:div.help [:i.icon-question-sign] "A maximum of 255 characters may be used"]])
     )))
