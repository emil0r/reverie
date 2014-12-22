(defproject reverie-core "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clout "2.0.0"]
                 [slingshot "0.12.1"]
                 [com.stuartsierra/component "0.2.2"]]
  :profiles {:dev {:dependencies [[midje "1.6.3"]
                                  [ring-mock "0.1.3"]
                                  [hiccup "1.0.5"]]
                   :plugins [[lein-midje "3.1.3"]]}})
