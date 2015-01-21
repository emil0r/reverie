(ns reverie.test.database.sql
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [joplin.core :as joplin]
            joplin.jdbc.database
            [reverie.database :as db]
            [reverie.database.sql :as sql]
            reverie.sql.objects.text
            reverie.sql.objects.image
            [reverie.page :as page]
            [reverie.object :as object]
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

(defn get-db []
  (assoc (sql/database {:default db-spec
                        :two db-spec-two})
    :system (component/start (sys/map->ReverieSystem {}))))

(let [db (component/start (get-db))]
  (try
    ;; (println (db/query db {:select [6]}))
    ;; (println (db/query db :two {:select [:*]
    ;;                             :from [:reverie_page]
    ;;                             :limit :?limit}
    ;;                    {:limit 1}))
    ;;(clojure.pprint/pprint (page/get-page db 2 true))
    ;;(clojure.pprint/pprint (object/get-objects db (page/page {:id 1})))
    ;;(println (db/query {:select [:*], :from [:reverie_text], :where [:in :object_id [1]]}))
    (catch Exception e
      (println e)))
  (component/stop db))




(defn seed! []
  (let [jmap {:db {:type :sql
                   :url (str "jdbc:postgresql:"
                             "//localhost:5432/dev_reverie"
                             "?user=" "devuser"
                             "&password=" "devuser")}
              :migrator ["resources/migrations/postgresql"
                         "src/reverie/sql/objects/migrations/text/"
                         "src/reverie/sql/objects/migrations/image/"]}]
    (joplin/rollback-db jmap 9000))
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
         (map (juxt :serial :name) (db/get-pages db))
         => [[1 "Main"] [1 "Main"] [2 "Baz"] [2 "Baz"] [3 "Bar"] [3 "Bar"]])
   (fact "get-pages published=true"
         (map (juxt :serial :name) (db/get-pages db true))
         => [[1 "Main"] [2 "Baz"] [3 "Bar"]])
   (fact "get-pages-by-route"
         (sort (map (juxt first #(-> % second :version))
                    (db/get-pages-by-route db)))
         => [["/" 0] ["/" 1] ["/bar" 0] ["/bar" 1] ["/baz" 0] ["/baz" 1]])
   (fact "get-page by id"
         ((juxt :serial :name :version) (db/get-page db 1))
         => [1 "Main" 0])
   (fact "get-page by serial"
         ((juxt :serial :name :version) (db/get-page db 1 true))
         => [1 "Main" 1])
   (fact "get-page by serial (nothing found)"
         ((juxt :serial :name :version) (db/get-page db 4 true))
         => [nil nil nil])
   (fact "get-children"
         (map (juxt :serial :name :version)
              (db/get-children db (db/get-page db 1)))
         => [[2 "Baz" 0] [3 "Bar" 0]])
   (fact "get-children-count"
         (db/get-children-count db (db/get-page db 1))
         => 2)
   (fact "get-children-count (published)"
         (db/get-children-count db (db/get-page db 1 true))
         => 2)
   (component/stop db)))


(fact
 "objects"
 (seed!)
 (let [db (get-db)]
  (fact "objects (text)"
        (let [p (db/get-page db 3)]
          (map :name (page/objects p)))
        => [:reverie/image :reverie/text :reverie/text])
  (component/stop db)))


(fact
 "!"
 (seed!)
 (let [db (get-db)]
   (fact "add page"
         (db/add-page! db {:parent 1 :title "" :name "Test page 1"
                           :route "/test-page-1" :template :foobaz
                           :type :page :app ""})
         (page/name (db/get-page db 4 false)) => "Test page 1")
   (fact "update page"
         (db/update-page! db
                          (-> (db/get-page db 1 false)
                              :id)
                          {:name "Test update-page!"})
         (page/name (db/get-page db 1 false)) => "Test update-page!")
   (fact "add object"
         (db/add-object! db {:page_id 1 :name "reverie/text"
                             :area "a" :route ""
                             :properties {:text "foobar"}})
         (-> (db/get-page db 1)
             page/objects
             last
             :properties
             :text)
         => "foobar")
   (fact "update object"
         (db/update-object! db 1 {:text "foobar"})
         (-> (db/get-page db 1)
             page/objects
             last
             :properties
             :text)
         => "foobar")
   (component/stop db)))


(fact
 "movement!"
 (let [db (get-db)]
   (fact "same level (move-page)"
         (fact ":before"
               (seed!)
               (db/move-page! db 5 3 :before)
               (map
                (juxt :order :name)
                (page/children (db/get-page db 1 false)))
               => [[1 "Bar"] [2 "Baz"]])
         (fact ":after"
               (seed!)
               (db/move-page! db 3 5 :after)
               (map
                (juxt :order :name)
                (page/children (db/get-page db 1 false)))
               => [[1 "Bar"] [2 "Baz"]]))
   (fact "another level (move-page)"
         (fact ":before"
               (seed!)
               (let [{:keys [id]} (db/add-page!
                                   db {:parent 2 :title ""
                                       :name "Test page 1"
                                       :route "/baz/test-page-1"
                                       :template :foobaz
                                       :type :page :app ""})]
                 (db/move-page! db id 5 :before)
                 (map
                  (juxt :order :name)
                  (page/children (db/get-page db 1 false))))
               => [[1 "Baz"] [2 "Test page 1"] [3 "Bar"]])
         (fact ":after"
               (seed!)
               (let [{:keys [id]} (db/add-page!
                                   db {:parent 2 :title ""
                                       :name "Test page 1"
                                       :route "/baz/test-page-1"
                                       :template :foobaz
                                       :type :page :app ""})]
                 (db/move-page! db id 5 :after)
                 (map
                  (juxt :order :name)
                  (page/children (db/get-page db 1 false))))
               => [[1 "Baz"] [2 "Bar"] [3 "Test page 1"]]))))
