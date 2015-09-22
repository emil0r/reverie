(defproject reverie-sql "0.7.0-SNAPSHOT"
  :description "The SQL backbone of reverie, a CMS written in Clojure"
  :url "http://reveriecms.org"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [reverie-core "0.7.0-SNAPSHOT"]
                 [honeysql "0.6.1"]
                 [yesql "0.5.0"]
                 [ez-database "0.3.0"]
                 [org.clojure/java.jdbc "0.4.1"]
                 [joplin.jdbc "0.3.3"]
                 [com.jolbox/bonecp "0.8.0.RELEASE"]]
  :profiles {:dev {:dependencies [[org.postgresql/postgresql "9.3-1102-jdbc41"]
                                  [midje "1.6.3"]]}})
