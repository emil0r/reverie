(ns reverie.site.seed
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [reverie.database :as db]
            [reverie.system :as sys]))

(defn seed! []
  (let [db (sys/get-db)
        seed (slurp (io/resource "seeds/postgresql/seed.sql"))]
    (try
      (doseq [line (str/split-lines seed)]
        (if-not (.startsWith line "--")
          (db/query! db line)))
      (catch Exception e
        (println e)))))


(comment
  ;; seed the database
  (seed!)
  )
