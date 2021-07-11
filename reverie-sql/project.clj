(defproject reverie-sql "0.9.0-alpha8"

  :description "The SQL backbone of reverie"

  :url "http://reveriecms.com"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [;;core
                 [org.clojure/clojure "1.10.1"]

                 ;; reverie
                 [reverie-core "0.9.0-alpha8"]

                 ;; database libraries
                 [org.clojure/java.jdbc "0.7.8"]
                 [yesql "0.5.3"]
                 [ez-database "0.6.0"]
                 [ez-form "0.7.3"]

                 ;; database migrations
                 [migratus "1.2.6"]
                 [com.fzakaria/slf4j-timbre "0.3.12"]

                 ;; connection pool
                 [hikari-cp "2.6.0"]]

  :profiles {:dev {:dependencies [[org.postgresql/postgresql "42.2.5"]
                                  [midje "1.9.4"]]}})
