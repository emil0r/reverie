(ns reverie.site.core
  (:gen-class)
  (:require [migratus.core :as migratus]
            [reverie.command :as command]
            [reverie.server :as server]
            [taoensso.timbre :as log]))

(def opts {:reverie.settings/path "settings.edn"
           :reverie.system/load-namespaces ['reverie.batteries.objects
                                            'reverie.site.templates
                                            'reverie.site.apps
                                            'reverie.site.endpoints]})

(defn start-server [opts]
  (server/start opts []))

(defn -main [& args]
  ;; run commands first. if a command is sent in the system exits after the command has run
  (command/run-command opts args)
  ;; if we manage to continue, we start the server 
  (start-server opts))

(comment

  ;; run this for running the server in dev mode through the REPL
  (do
    (server/stop)
    (start-server))


  ;; copy/paste and change the commented out migration to suit your needs
  ;; run in the REPL as necessary during development
  (let [datasource (get-in @server/system [:database :db-specs :default :datasource])
        get-migrator-map (fn [[table path]]
                           {:store :database
                            :db datasource
                            :migration-table-name table
                            :migration-dir path
                            :name table})
        mmaps (map get-migrator-map
                   (array-map
                    ;;"migrations_module_reverie_blog" "migrations/modules/blog/"
                    "migrations_reverie_reset_password" "reverie/batteries/objects/migrations/reset-password"))]
    
    



    ;; IMPORTANT NOTE: this has destructive side effects in the sense
    ;; of wiping out previously applied migrations.
    ;; due to the nature of the weak binding between objects and the table
    ;; keeping track of them this can cause problems with reverie trying
    ;; to pull in objects that no longer exists. to fix that you would need
    ;; to manually delete the reference for that object in the reverie_object
    ;; table
    (doseq [mmap mmaps]
      (log/debug "Rolling back" (:name mmap))
      (with-out-str (migratus/rollback mmap))
      (log/debug "Migrating" (:name mmap))
      (with-out-str (migratus/migrate mmap))
      ))
  )

