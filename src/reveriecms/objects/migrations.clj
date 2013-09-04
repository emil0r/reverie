(ns reveriecms.objects.migrations
  (:refer-clojure :exclude [alter defonce drop bigint boolean char double float time])
  (:use (lobos [migration :only [defmigration]] core schema)))


(defmigration init-object-text
  (up [] (create (table :text
                        (integer :id :primary-key :auto-inc :not-null)
                        (text :text :not-null (default ""))
                        (integer :object_id [:refer :object :id] :not-null))))
  (down [] (drop (table :test))))


(defn migrate-objects []
  (binding [lobos.migration/*migrations-namespace* 'reveriecms.objects.migrations]
    (lobos.core/migrate))
  (binding [lobos.migration/*migrations-namespace* 'lobos.migrations]))
