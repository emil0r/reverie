(defproject reverie-sql "0.9.0-SNAPSHOT"
  :description "The SQL backbone of reverie; a CMS for power users"
  :url "http://reveriecms.org"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [;;core
                 [org.clojure/clojure "1.9.0-alpha16"]

                 ;; reverie
                 [reverie-core "0.9.0-SNAPSHOT"]

                 ;; database libraries
                 [org.clojure/java.jdbc "0.6.1"]
                 [yesql "0.5.3"]
                 [ez-database "0.6.0"]
                 [ez-form "0.7.3"]

                 ;; database migrations
                 [joplin.core "0.3.10"]
                 [joplin.jdbc "0.3.10"]

                 ;; connection pool
                 [hikari-cp "1.6.1"]]

  :profiles {:dev {:dependencies [[org.postgresql/postgresql "42.0.0"]
                                  [midje "1.9.0-alpha6"]]}})
