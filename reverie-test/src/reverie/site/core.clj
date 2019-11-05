(ns reverie.site.core
  (:gen-class)
  (:require [reverie.command :as command]
            [reverie.server :as server]))


(defn -main [& args]
  ;; run commands first. if a command is sent in the system exits after the command has run
  (command/run-command "settings.edn" args)
  ;; if we manage to continue, we start the server 
  (server/start {:reverie.settings/path "settings.edn"
                 :reverie.system/load-namespaces ['reverie.batteries.objects
                                                  'reverie.site.templates
                                                  'reverie.site.apps
                                                  'reverie.site.endpoints]} []))

(comment

  ;; run this for running the server in dev mode through the REPL
  (do
    (server/stop)
    (server/start {:reverie.settings/path "settings.edn"
                   :reverie.system/load-namespaces ['reverie.batteries.objects
                                                    'reverie.site.templates
                                                    'reverie.site.apps
                                                    'reverie.site.endpoints]} []))


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

