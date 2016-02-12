(defproject reverie-test "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [reverie-core "0.7.0-SNAPSHOT"]
                 [reverie-sql "0.7.0-SNAPSHOT"]
                 [reverie-batteries "0.3.0-SNAPSHOT"]
                 [http-kit "2.1.19"]
                 [org.postgresql/postgresql "9.3-1102-jdbc41"]]
    :profiles {:dev {:dependencies [[midje "1.6.3"]
                                    [ring-mock "0.1.5"]
                                    [spyscope "0.1.5"]
                                    [org.clojure/tools.namespace "0.2.9"]]
                     :injections [(require 'spyscope.core)
                                  (require 'spyscope.repl)
                                  (require '[clojure.tools.namespace.repl :refer [refresh]])]
                     :resource-paths ["../reverie-sql/resources"]
                     :plugins [[lein-midje "3.1.3"]]}})
