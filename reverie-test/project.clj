(defproject reverie-test "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [reverie-core "0.8.0-alpha6"]
                 [reverie-sql "0.8.0-alpha6"]
                 [reverie-batteries "0.3.1"]
                 [reverie-redis "0.1.0"]
                 [reverie-blog "0.2.2"]
                 [reverie-blockade "0.11.0"]
                 [http-kit "2.1.19"]
                 [org.postgresql/postgresql "9.3-1102-jdbc41"]]

  :main reverie.site.core

  :uberjar-name "reverie-test.jar"

  :profiles {:uberjar {:aot [reverie.site.core]}
             :dev {:dependencies [[midje "1.6.3"]
                                  [ring-mock "0.1.5"]
                                  [spyscope "0.1.5"]
                                  [org.clojure/tools.namespace "0.2.9"]]
                   :injections [(require 'spyscope.core)
                                (require 'spyscope.repl)
                                (require '[clojure.tools.namespace.repl :refer [refresh]])]
                   :resource-paths ["../reverie-sql/resources"]
                   :plugins [[lein-midje "3.1.3"]]}})
