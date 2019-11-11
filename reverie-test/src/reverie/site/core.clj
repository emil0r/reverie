(ns reverie.site.core
  (:gen-class)
  (:require [migratus.core :as migratus]
            [reverie.dev.migration :as migration]
            [reverie.dev.object :as object]
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
    (start-server opts))


  ;; for manual testing
  (object/create {:override? true} 'reverie/testus)
  (migration/migrate :object 'reverie/testus)
  (migration/rollback :object 'reverie/testus)
  (object/add-migration 'reverie/testus "foobar")
  (object/remove-migration 'reverie/testus "foobar")
  )

