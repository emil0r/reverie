(ns reverie.site.core
  (:gen-class)
  (:require [clojure.edn :as edn]
            [joplin.core :as joplin]
            [joplin.jdbc.database]
            [reverie.site.init :as init]))


(defn read-args [args]
  (reduce (fn [out [arg value]]
            (assoc out (edn/read-string arg) (edn/read-string value)))
          {} (partition 2 args)))

(defn -main [& args]
  (init/init "settings.edn" (read-args args)))

(comment

  ;; run this for running the server in dev mode through the REPL
  (do
    (init/stop)
    (init/init "settings.edn"))


  ;; copy/paste and change the commented out migration to suit your needs
  ;; run in the REPL as necessary during development
  (let [mmaps (map (fn [[table path]]
                     {:db {:type :sql
                           :migrations-table table
                           :url (str "jdbc:postgresql:"
                                     "//localhost:5432/dev_reverie"
                                     "?user=" "devuser"
                                     "&password=" "devuser")}
                      :migrator path})
                   (array-map))]
                    ;;"migrations_module_reverie_blog" "resources/migrations/modules/blog/"
                    ;;"migrations_reverie_reset_password" "src/reverie/batteries/objects/migrations/reset-password"



    ;; IMPORTANT NOTE: this has destructive side effects in the sense
    ;; of wiping out previously applied migrations.
    ;; due to the nature of the weak binding between objects and the table
    ;; keeping track of them this can cause problems with reverie trying
    ;; to pull in objects that no longer exists. to fix that you would need
    ;; to manually delete the reference for that object in the reverie_object
    ;; table
    (doseq [mmap mmaps]
      (joplin/rollback-db mmap 1)
      (joplin/migrate-db mmap))))

