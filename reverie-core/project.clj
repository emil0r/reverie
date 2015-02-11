(defproject reverie-core "0.1.0-SNAPSHOT"
  :description ""
  :url ""
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [ring/ring-core "1.3.1"]
                 [clout "2.0.0"]
                 [bultitude "0.1.7"]
                 [slingshot "0.12.1"]
                 [buddy/buddy-core "0.3.0"]
                 [buddy/buddy-hashers "0.3.0"]
                 [buddy/buddy-auth "0.3.0-SNAPSHOT"]
                 [org.clojure/core.match "0.2.1"]
                 [com.taoensso/timbre "3.3.1"]
                 [com.stuartsierra/component "0.2.2"]
                 [hiccup "1.0.5"]
                 [vlad "1.1.0"]
                 [lib-noir "0.9.5"]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]
                                  [ring-mock "0.1.3"]
                                  [reverie-sql "0.1.0-SNAPSHOT"]
                                  [reverie-batteries "0.1.0-SNAPSHOT"]
                                  [spyscope "0.1.5"]
                                  [http-kit "2.1.16"]
                                  [org.clojure/tools.namespace "0.2.9"]
                                  [org.postgresql/postgresql "9.3-1102-jdbc41"]]
                   :injections [(require 'spyscope.core)
                                (require 'spyscope.repl)
                                (require '[clojure.tools.namespace.repl :refer [refresh]])]
                   :resource-paths ["../reverie-sql/resources"]
                   :plugins [[lein-midje "3.1.3"]]}})
