(ns reverie.test.dummy
  (:require [com.stuartsierra.component :as component]
            [ez-database.core :as db]
            [reverie.database :as rev.db]
            [reverie.object :as object]
            [reverie.page :as page]
            [reverie.system :as sys]
            [midje.sweet :refer :all]))

(defn get-system []
  (let [system (sys/map->ReverieSystem {})]
    (component/start system)))



(defn- get-new-page [data db]
  (case (:type data)
    :page (page/page (assoc data :database db))
    :raw (page/raw-page (assoc data :database db))
    :app (page/app-page (assoc data :database db))
    nil))

(defrecord DummyDatabase [pages objects users]
  db/IEzDatabase
  (query [db query] nil)
  (query [db query args] nil)
  (query! [db query] nil)
  (query! [db query args])

  rev.db/IDatabase
  (get-pages [db] pages)
  (get-pages-by-route [db]
    (map (fn [{:keys [route id template type app name]}]
           [route {:id id :template template
                   :type type :app app :name name}]) pages))
  (get-page [db id]
    (get-new-page (first (filter #(= id (:id %)) pages)) db))
  (get-children [db page]
    (get-new-page (filter #(= (page/id page) (:parent %)) pages) db))

  (get-objects [db page]
    (sort-by object/order
             (map #(assoc (object/map->ReverieObject %)
                     :page page
                     :database db)
                  (filter
                   #(= (page/id page) (:page %))
                   objects)))))



(defn get-db []
  (map->DummyDatabase
   {:pages [{:template :testus :id 1 :name "Test page"
             :title "My test page" :parent nil :children [2]
             :route "/" :type :page :app nil}
            {:template :testus :id 2 :name "Test page"
             :title "My test page" :parent 1 :children nil
             :route "/foo" :type :app :app :foobar}]
    :objects [{:name :text :id 1 :area :a :page 1
               :order 1 :properties {:text "My text"}}
              {:name :image :id 2 :area :a :page 1
               :app-path "/bar"
               :order 2 :properties {:src "/images/test.png"
                                           :alt "Alt"
                                           :title "Title"}}
              {:name :text :id 1 :area :a :page 2
               :order -1 :properties {:text "My text"}}
              {:name :image :id 2 :area :a :page 2
               :app-path "/bar"
               :order 1 :properties {:src "/images/test.png"
                                           :alt "Alt"
                                           :title "Title"}}]
    :users [{:id 1 :username "test@test.com"
             :password "foobar" :email "test@test.com"}]}))


(fact
 "test database"
 (let [db (get-db)]
  (fact "page"
        (:name (rev.db/get-page db 1)) => "Test page")
  (fact "objects"
        (map :name (rev.db/get-objects db (rev.db/get-page db 1)))
        => [:text :image])))
