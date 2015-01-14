(ns reverie.test.database.sql
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [reverie.database :as db]
            [reverie.database.sql :as sql]
            [reverie.page :as page]
            [reverie.system :as sys]
            [midje.sweet :refer :all]))

(def db-spec {:classname "org.postgresql.Driver"
              :subprotocol "postgresql"
              :subname "//localhost:5432/dev_reverie"
              :user "devuser"
              :password "devuser"})

(def db-spec-two
  {:classname "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname "//localhost:5432/dev_reverie"
   :user "devuser"
   :password "devuser"})

(defn- get-db []
  (assoc (sql/database {:default db-spec
                        :two db-spec-two})
    :system (component/start (sys/map->ReverieSystem {}))))

(let [db (component/start (get-db))]
  (try
    (println (db/query db {:select [6]}))
    (println (db/query db :two {:select [:*]
                                :from [:reverie_page]
                                :limit :?limit}
                       {:limit 1}))
    (catch Exception e
      (println e)))
  (component/stop db))




(defn seed! []
  (let [jmap {:db {:type :sql
                   :url (str "jdbc:postgresql:"
                             "//localhost:5432/dev_reverie"
                             "?user=" "devuser"
                             "&password=" "devuser")}
              :migrator (str "resources/migrations/postgresql")}]
    (joplin/rollback-db jmap))
  (let [db (component/start (get-db))
        seed (slurp (io/resource "seeds/postgresql/seed.sql"))]
    (try
      (doseq [line (str/split-lines seed)]
        (if-not (.startsWith line "--")
          (db/query! db line)))
      (catch Exception e
        (println e)))
    (component/stop db)))



(fact
 "pages"
 (seed!)
 (let [db (component/start (get-db))]
   (fact "get-pages"
         (map (juxt :serial :name) (page/get-pages db))
         => [[1 "Main"] [1 "Main"] [2 "Baz"] [3 "Bar"]])
   (fact "get-pages published=true"
         (map (juxt :serial :name) (page/get-pages db true))
         => [[1 "Main"]])
   (fact "get-pages-by-route"
         (sort (map (juxt first #(-> % second :version))
                    (page/get-pages-by-route db)))
         => [["/" 0] ["/" 1] ["/bar" 0] ["/baz" 0]])
   (fact "get-page by id"
         ((juxt :serial :name :version) (page/get-page db 1))
         => [1 "Main" 0])
   (fact "get-page by serial"
         ((juxt :serial :name :version) (page/get-page db 1 true))
         => [1 "Main" 1])
   (fact "get-children"
         (map (juxt :serial :name :version)
              (page/get-children db (page/get-page db 1)))
         => [[2 "Baz" 0] [3 "Bar" 0]])
   (fact "get-children-count"
         (page/get-children-count db (page/get-page db 1))
         => 2)
   (fact "get-children-count (published)"
         (page/get-children-count db (page/get-page db 1 true))
         => 0)
   (component/stop db)))
