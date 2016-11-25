(defproject reverie-sql "0.8.0-SNAPSHOT"
  :description "The SQL backbone of reverie; a CMS for power users"
  :url "http://reveriecms.org"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [;;core
                 [org.clojure/clojure "1.8.0"]

                 ;; reverie
                 [reverie-core "0.8.0-SNAPSHOT"]

                 ;; database libraries
                 [org.clojure/java.jdbc "0.4.1"]
                 [honeysql "0.6.3"]
                 [yesql "0.5.2"]
                 [ez-database "0.5.3"]
                 [ez-form "0.7.0"]

                 ;; database migrations
                 [joplin.jdbc "0.3.4"]

                 ;; connection pool
                 [hikari-cp "1.6.1"]]

  :profiles {:dev {:dependencies [[org.postgresql/postgresql "9.3-1102-jdbc41"]
                                  [midje "1.6.3"]]}})
