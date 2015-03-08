(defproject reverie-core "0.1.0-SNAPSHOT"
  :description ""
  :url ""
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [ring/ring-anti-forgery "1.0.0"]
                 [ring/ring-core "1.3.1"]
                 [clout "2.1.0"]
                 [bultitude "0.2.6"]
                 [slingshot "0.12.2"]
                 [buddy/buddy-core "0.4.0"]
                 [buddy/buddy-hashers "0.4.0"]
                 [buddy/buddy-auth "0.4.0"]
                 [org.clojure/core.match "0.2.2"]
                 [com.taoensso/timbre "3.4.0"]
                 [com.stuartsierra/component "0.2.2"]
                 [hiccup "1.0.5"]
                 [vlad "1.1.0"]
                 [lib-noir "0.9.5"]
                 [ez-web "0.2.4"]
                 [ez-image "1.0.2"]
                 [me.raynes/fs "1.4.6"]
                 [im.chit/cronj "1.4.3"]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]
                                  [ring-mock "0.1.5"]
                                  [reverie-sql "0.1.0-SNAPSHOT"]
                                  [reverie-batteries "0.1.0-SNAPSHOT"]
                                  [spyscope "0.1.5"]
                                  [http-kit "2.1.19"]
                                  [org.clojure/tools.namespace "0.2.9"]
                                  [org.postgresql/postgresql "9.3-1102-jdbc41"]]
                   :injections [(require 'spyscope.core)
                                (require 'spyscope.repl)
                                (require '[clojure.tools.namespace.repl :refer [refresh]])]
                   :resource-paths ["../reverie-sql/resources"]
                   :plugins [[lein-midje "3.1.3"]
                             [lein-cljsbuild "1.0.5"]]}})
