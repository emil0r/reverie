(defproject reverie-test "0.1.0-SNAPSHOT"
  :description "Just for manual tests"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [reverie-core "0.9.0-SNAPSHOT"]
                 [reverie-sql "0.9.0-SNAPSHOT"]
                 [reverie-batteries "0.5.0"]
                 [reverie-redis "0.1.0"]
                 [reverie-blog "0.3.4"]
                 [reverie-blockade "0.11.0"]
                 [http-kit "2.3.0"]
                 [org.postgresql/postgresql "42.2.8"]
                 [hikari-cp "2.9.0"]
                 [ez-database "0.7.0-alpha2"]

                 ;; for testing
                 [ring/ring-json "0.4.0"]]

  :main reverie.site.core

  :uberjar-name "reverie-test.jar"

  :repl-options {:timeout 120000}

  :profiles {:uberjar {:aot [reverie.site.core]}
             :dev {:dependencies [[midje "1.9.4"]
                                  [ring-mock "0.1.5"]
                                  ;;[spyscope "0.1.7-SNAPSHOT"]
                                  ;;[org.clojure/tools.namespace "0.2.10"]
                                  ]
                   ;; :injections [(require 'spyscope.core)
                   ;;              (require 'spyscope.repl)
                   ;;              (require '[clojure.tools.namespace.repl :refer [refresh]])]
                   :resource-paths ["../reverie-sql/resources"]
                   :plugins [[lein-midje "3.1.3"]]}})
