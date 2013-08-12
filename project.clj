(defproject reverie "0.1.0-SNAPSHOT"
  :description "A sane CMS"
  :url "http://reveriecms.org"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [ring "1.1.8"]
                 [korma "0.3.0-RC5"]
                 [fs "1.3.2"]
                 [clout "1.1.0"]
                 [slingshot "0.10.3"]
                 [bultitude "0.1.5"]
                 [lobos "1.0.0-beta1"]]
  :profiles {:dev {:dependencies [[com.datomic/datomic-free "0.8.3848"]
                                  [midje "1.6-alpha1"]
                                  [com.stuartsierra/lazytest "1.2.3"]
                                  [ring-mock "0.1.3"]
                                  [org.postgresql/postgresql "9.2-1002-jdbc4"]]}}
  :repositories {"stuart" "http://stuartsierra.com/maven2"})
