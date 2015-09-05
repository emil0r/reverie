(ns reverie.test.modules.auth
  (:require [clojure.set :as set]
            [com.stuartsierra.component :as component]
            [reverie.core :refer [defmodule]]
            [reverie.database :as db]
            [reverie.module :as module]
            [reverie.modules.role :as role]
            [reverie.modules.auth :as auth]
            [reverie.render :as render]
            [reverie.route :as route]
            [reverie.test.database.sql-helpers :refer [get-db]]
            [reverie.system :as sys]
            [midje.sweet :refer :all]))



(fact
 "Roles added properly"
 (let [db (component/start (get-db))]
   (try
     (-> {:database db
          :system (component/start (sys/map->ReverieSystem {}))}
         (role/map->RoleManager)
         (component/start))
     (->> (db/query db "SELECT name FROM auth_role;")
          (map #(-> % :name keyword))
          sort
          set
          (set/intersection #{:admin :staff :user}))
     => #{:admin :staff :user}
     (catch Exception e
       (println e)))
   (component/stop db)))

(defn module-hi [request module params]
  [:div "Hi there!"])


(fact "render module"
      (defmodule testus {} [["/" {:get module-hi}]])
      (let [mod (assoc (-> @sys/storage :modules :testus :module)
                  :route (route/route ["/admin/frame/module/testus"]))]
        (render/render mod {:uri "/admin/frame/module/testus" :request-method :get})
        => [:div "Hi there!"]))
