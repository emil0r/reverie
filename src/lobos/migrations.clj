(ns lobos.migrations
  (:refer-clojure :exclude [alter defonce drop bigint boolean char double float time])
  (:use (lobos [migration :only [defmigration]] core schema)))


(defmigration init-page
  (up [] (create (table :page
                        (integer :id :primary-key :auto-inc :not-null)
                        (varchar :name 255 :not-null)
                        (varchar :title 255 :not-null)
                        (varchar :template 255 :not-null)
                        (varchar :uri 2048 :not-null)
                        (integer :parent)
                        (integer :order :not-null)
                        (integer :version :not-null)
                        
                        (index :page [:uri])
                        (index :page [:name])
                        (index :page [:parent]))))
  (down [] (drop (table :page))))

(defmigration init-page-attributes
  (up [] (create (table :page-attributes
                        (integer :id :primary-key :auto-inc :not-null)
                        (varchar :name 255 :not-null)
                        (integer :page-id :refer :page))))
  (down [] (drop (table :page-attributes))))

(defmigration init-role
  (up [] (create (table :role
                        (integer :id :primary-key :auto-inc :not-null)
                        (varchar :name 255 :not-null))))
    (down [] (drop (table :role))))
