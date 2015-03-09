(defproject reverie-sql "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [honeysql "0.4.3"]
                 [yesql "0.5.0-rc1"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [joplin.jdbc "0.2.9"]
                 [com.jolbox/bonecp "0.8.0.RELEASE"]]
  :profiles {:dev {:dependencies [[org.postgresql/postgresql "9.3-1102-jdbc41"]
                                  [midje "1.6.3"]]}})
