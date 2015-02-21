(ns reverie.test.database.sql
  (:require [com.stuartsierra.component :as component]
            [reverie.database :as db]
            [reverie.database.sql :as sql]
            [reverie.page :as page]
            [reverie.object :as object]
            [reverie.publish :as publish]
            [reverie.system :as sys]
            [reverie.test.database.sql-helpers :refer [get-db seed!]]
            [midje.sweet :refer :all]))


(let [db (component/start (get-db))]
  (try
    ;; (doseq [i (range 100)]
    ;;   (future #spy/t (db/query db {:select [i]})))
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
 (let [db (component/start (get-db))]
  (fact "objects (text)"
        (let [p (db/get-page db 3)]
          (map :name (page/objects p)))
        => [:reverie/image :reverie/text :reverie/text])
  (component/stop db)))


(fact
 "!"
 (seed!)
 (let [db (component/start (get-db))]
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
 "movement page!"
 (let [db (component/start (get-db))]
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
               => [[1 "Baz"] [2 "Bar"] [3 "Test page 1"]]))
   (component/stop db)))

(fact
 "movement object!"
 (let [db (component/start (get-db))]
   (fact "same level (move-object)"
         (fact ":up"
               (seed!)
               (db/move-object! db 11 :up)
               (map
                (juxt :id :order :name)
                (page/objects (db/get-page db 5)))
               => [[11 1 :reverie/text] [9 2 :reverie/text]])
         (fact ":down"
               (seed!)
               (db/move-object! db 9 :up)
               (map
                (juxt :id :order :name)
                (page/objects (db/get-page db 5)))
               => [[9 -1 :reverie/text] [11 1 :reverie/text]]))
   (fact "other page and area"
         (seed!)
         (db/move-object! db 9 3 :b)
         (->>
          (page/objects (db/get-page db 3))
          (filter (fn [{:keys [area]}] (= area :b)))
          (map (juxt :id :order :name :area)))
         => [[5 -1 :reverie/image :b] [7 1 :reverie/text :b] [9 2 :reverie/text :b]])
   (fact "move object to object"
         (fact "after"
               (seed!)
               (db/move-object-to-object! db 9 1 :after)
               (->>
                (page/objects (db/get-page db 1))
                (filter (fn [{:keys [area]}] (= area :a)))
                (map (juxt :id :order :name :area :page_id)))
               => [[1 1 :reverie/text :a 1] [9 2 :reverie/text :a 1]])
         (fact "before"
               (seed!)
               (db/move-object-to-object! db 9 1 :before)
               (->>
                (page/objects (db/get-page db 1))
                (filter (fn [{:keys [area]}] (= area :a)))
                (map (juxt :id :order :name :area :page_id)))
               => [[9 1 :reverie/text :a 1] [1 2 :reverie/text :a 1]]))
   (component/stop db)))


(fact "publishing"
      (seed!)
      (let [db (component/start (get-db))]
        (fact "publish"
              (publish/publish-page! db 1)
              (->>
               (db/query db {:select [:serial :version]
                             :from [:reverie_page]
                             :where [:= :serial 1]
                             :order-by [:version]})
               (map (juxt :serial :version)))
              => [[1 0] [1 1] [1 2]])
        (fact "add pages and publish (single)"
              (db/add-page! db {:parent 1 :title "" :name "Test page 1"
                                :template :foobaz
                                :type :page :app ""})
              (db/add-page! db {:parent 4 :title "" :name "Test page 2"
                                :template :foobaz
                                :type :page :app ""})
              (db/add-page! db {:parent 5 :title "" :name "Test page 3"
                                :template :foobaz
                                :type :page :app ""})
              (publish/publish-page! db 8)
              (->>
               (db/query db {:select [:serial :version]
                             :from [:reverie_page]
                             :where [:and
                                     [:in :serial [4 5 6]]
                                     [:= :version 1]]
                             :order-by [:version]})
               (map (juxt :serial :version)))
              => [[4 1]])
        (fact "publish (recursive)"
              (publish/publish-page! db 8 true)
              (->>
               (db/query db {:select [:serial :version]
                             :from [:reverie_page]
                             :where [:and
                                     [:in :serial [4 5 6]]
                                     [:= :version 1]]
                             :order-by [:version]})
               (map (juxt :serial :version)))
              => [[4 1] [5 1] [6 1]])
        (fact "unpublish!"
              (publish/unpublish-page! db 8)
              (->>
               (db/query db {:select [:serial :version]
                             :from [:reverie_page]
                             :where [:and
                                     [:in :serial [4 5 6]]
                                     [:= :version 1]]
                             :order-by [:version]})
               (map (juxt :serial :version)))
              => [])
        (fact "trash page!"
              (publish/trash-page! db 8)
              (->>
               (db/query db {:select [:serial :version]
                             :from [:reverie_page]
                             :where [:and
                                     [:in :serial [4 5 6]]
                                     [:= :version -1]]
                             :order-by [:version]})
               (map (juxt :serial :version)))
              => [[4 -1] [5 -1] [6 -1]])
        (fact "trash object!"
              (publish/trash-object! db 1)
              (count (page/objects (db/get-page db 1)))
              => 0)
        (component/stop db)))


(fact "insert!"
      (seed!)
      (let [db (component/start (get-db))]
        (try
          (db/query<! db {:insert-into :auth_group
                                 :values [{:name "asdf"}]})
          (catch Exception e
            (println e)))
        => [{:id 2 :name "asdf"}]
        (component/stop db)))
