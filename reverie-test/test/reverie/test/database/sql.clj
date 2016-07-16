(ns reverie.test.database.sql
  (:require [com.stuartsierra.component :as component]
            [ez-database.core :as db]
            [reverie.database :as rev.db]
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
         (map (juxt :serial :name) (rev.db/get-pages db))
         => [[1 "Main"] [1 "Main"] [2 "Baz"] [2 "Baz"] [3 "Bar"] [3 "Bar"]])
   (fact "get-pages published=true"
         (map (juxt :serial :name) (rev.db/get-pages db true))
         => [[1 "Main"] [2 "Baz"] [3 "Bar"]])
   (fact "get-pages-by-route"
         (sort (map (juxt first #(-> % second :version))
                    (rev.db/get-pages-by-route db)))
         => [["/" 0] ["/" 1] ["/bar" 0] ["/bar" 1] ["/baz" 0] ["/baz" 1]])
   (fact "get-page by id"
         ((juxt :serial :name :version) (rev.db/get-page db 1))
         => [1 "Main" 0])
   (fact "get-page by serial"
         ((juxt :serial :name :version) (rev.db/get-page db 1 true))
         => [1 "Main" 1])
   (fact "get-page by serial (nothing found)"
         ((juxt :serial :name :version) (rev.db/get-page db 4 true))
         => [nil nil nil])
   (fact "get-children"
         (map (juxt :serial :name :version)
              (rev.db/get-children db (rev.db/get-page db 1)))
         => [[2 "Baz" 0] [3 "Bar" 0]])
   (fact "get-children-count"
         (rev.db/get-children-count db (rev.db/get-page db 1))
         => 2)
   (fact "get-children-count (published)"
         (rev.db/get-children-count db (rev.db/get-page db 1 true))
         => 2)
   (component/stop db)))


(fact
 "objects"
 (seed!)
 (let [db (component/start (get-db))]
   (fact "objects (text)"
         (let [p (rev.db/get-page db 3)]
           (map :name (page/objects p)))
         => [:reverie/image :reverie/text :reverie/text])
   (component/stop db)))


(fact
 "!"
 (seed!)
 (let [db (component/start (get-db))]
   (fact "add page"
         (rev.db/add-page! db {:parent 1 :title "" :name "Test page 1"
                               :route "/test-page-1" :template :foobaz
                               :type :page :app ""})
         (page/name (rev.db/get-page db 4 false)) => "Test page 1")
   (fact "update page"
         (rev.db/update-page! db
                              (-> (rev.db/get-page db 1 false)
                                  :id)
                              {:name "Test update-page!"})
         (page/name (rev.db/get-page db 1 false)) => "Test update-page!")
   (fact "add object"
         (rev.db/add-object! db {:page_id 1 :name "reverie/text"
                                 :area "a" :route ""
                                 :properties {:text "foobar"}})
         (-> (rev.db/get-page db 1)
             page/objects
             last
             :properties
             :text)
         => "foobar")
   (fact "update object"
         (rev.db/update-object! db 1 {:text "foobar"})
         (-> (rev.db/get-page db 1)
             page/objects
             last
             :properties
             :text)
         => "foobar")
   (component/stop db)))

(fact
 "authorization added"
 (seed!)
 (let [db (component/start (get-db))
       serial (:serial (rev.db/add-page! db {:parent 1 :title ""
                                             :name "Test page 1"
                                             :route "/test-page-1"
                                             :template :foobaz
                                             :type :page :app ""}))]

   (-> (db/query db
                 {:select [:*]
                  :from [:auth_storage]
                  :where [:and
                          [:= :what "reverie.page/Page"]
                          [:= :id_int serial]]})
       first :id_int) => serial
       (component/stop db)))

(fact
 "movement page!"
 (let [db (component/start (get-db))]
   (fact "same level (move-page)"
         (fact ":before"
               (seed!)
               (rev.db/move-page! db 5 3 :before)
               (map
                (juxt :order :name :parent)
                (page/children (rev.db/get-page db 1 false)))
               => [[1 "Bar" 1] [2 "Baz" 1]])
         (fact ":after"
               (seed!)
               (rev.db/move-page! db 3 5 :after)
               (map
                (juxt :order :name :parent)
                (page/children (rev.db/get-page db 1 false)))
               => [[1 "Bar" 1] [2 "Baz" 1]]))
   (fact "another level (move-page)"
         (fact ":before"
               (seed!)
               (let [{:keys [id]} (rev.db/add-page!
                                   db {:parent 2 :title ""
                                       :name "Test page 1"
                                       :route "/baz/test-page-1"
                                       :template :foobaz
                                       :type :page :app ""})]
                 (rev.db/move-page! db id 5 :before)
                 (map
                  (juxt :order :name :parent)
                  (page/children (rev.db/get-page db 1 false))))
               => [[1 "Baz" 1] [2 "Test page 1" 1] [3 "Bar" 1]])
         (fact ":after"
               (seed!)
               (let [{:keys [id]} (rev.db/add-page!
                                   db {:parent 2 :title ""
                                       :name "Test page 1"
                                       :route "/baz/test-page-1"
                                       :template :foobaz
                                       :type :page :app ""})]
                 (rev.db/move-page! db id 5 :after)
                 (map
                  (juxt :order :name :parent)
                  (page/children (rev.db/get-page db 1 false))))
               => [[1 "Baz" 1] [2 "Bar" 1] [3 "Test page 1" 1]])
         (fact ":over"
               (seed!)
               (let [{:keys [id]} (rev.db/add-page!
                                   db {:parent 2 :title ""
                                       :name "Test page 1"
                                       :route "/baz/test-page-1"
                                       :template :foobaz
                                       :type :page :app ""})]
                 (rev.db/move-page! db 5 3 :over)
                 (map
                  (juxt :order :name :parent)
                  (page/children (rev.db/get-page db 2 false))))
               => [[1 "Test page 1" 2] [2 "Bar" 2]]))
   (component/stop db)))

(fact
 "movement object!"
 (let [db (component/start (get-db))]
   (fact "same level (move-object)"
         (fact ":up"
               (seed!)
               (rev.db/move-object! db 11 :up)
               (map
                (juxt :id :order :name)
                (page/objects (rev.db/get-page db 5)))
               => [[11 1 :reverie/text] [9 2 :reverie/text]])
         (fact ":down"
               (seed!)
               (rev.db/move-object! db 9 :down)
               (map
                (juxt :id :order :name)
                (page/objects (rev.db/get-page db 5)))
               => [[11 1 :reverie/text] [9 2 :reverie/text]]))
   (fact "other page and area"
         (seed!)
         (rev.db/move-object! db 9 3 :b)
         (->>
          (page/objects (rev.db/get-page db 3))
          (filter (fn [{:keys [area]}] (= area :b)))
          (map (juxt :id :order :name :area)))
         => [[5 -1 :reverie/image :b] [7 1 :reverie/text :b] [9 2 :reverie/text :b]])
   (fact "move object to object"
         (fact "after"
               (seed!)
               (rev.db/move-object-to-object! db 9 1 :after)
               (->>
                (page/objects (rev.db/get-page db 1))
                (filter (fn [{:keys [area]}] (= area :a)))
                (map (juxt :id :order :name :area :page_id)))
               => [[1 1 :reverie/text :a 1] [9 2 :reverie/text :a 1]])
         (fact "before"
               (seed!)
               (rev.db/move-object-to-object! db 9 1 :before)
               (->>
                (page/objects (rev.db/get-page db 1))
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
              (rev.db/add-page! db {:parent 1 :title "" :name "Test page 1"
                                    :template :foobaz
                                    :type :page :app ""})
              (rev.db/add-page! db {:parent 4 :title "" :name "Test page 2"
                                    :template :foobaz
                                    :type :page :app ""})
              (rev.db/add-page! db {:parent 5 :title "" :name "Test page 3"
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
              (count (page/objects (rev.db/get-page db 1)))
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
