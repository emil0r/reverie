(defproject reverie-core "0.9.0-alpha5"

  :description "The core of reverie"

  :url "http://reveriecms.com"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [;; core
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/core.match "0.3.0"]
                 [org.clojure/core.async "0.4.500"]
                 [org.clojure/core.memoize "0.7.2"]

                 ;; structure
                 [com.stuartsierra/component "0.4.0"]

                 ;; web
                 [ring/ring-anti-forgery "1.3.0"]
                 [ring/ring-core "1.7.1"]
                 [clout "2.2.1"]
                 [hiccup "1.0.5"]
                 [vlad "3.3.2"]
                 [lib-noir "0.9.9"]
                 [ez-web "0.3.0"]
                 [ez-image "1.0.4"]

                 ;; errors
                 [expound "0.7.2"]

                 ;; time
                 [clj-time "0.15.2"]

                 ;; email
                 [ez-email "0.1.0"]

                 ;; reverie specific
                 [timofreiberg/bultitude "0.3.1"]

                 ;; exception handling
                 [slingshot "0.12.2"]

                 ;; hashing, digests, crypto, etc
                 [buddy "2.0.0"]
                 [digest "1.4.9"]

                 ;; logging
                 [com.taoensso/timbre "4.10.0"]
                 [com.taoensso/encore "2.105.0"]

                 ;; i18n
                 [com.taoensso/tower "3.0.2"]

                 ;; filesystem
                 [clj-commons/fs "1.5.1"]

                 ;; scheduling
                 [im.chit/cronj "1.4.4"]

                 ;; schema
                 [prismatic/schema "1.1.9"]]

  :aot [reverie.AreaException
        reverie.CacheException
        reverie.DatabaseException
        reverie.MigrationException
        reverie.ModuleException
        reverie.ObjectException
        reverie.RenderException]

  :plugins [[lein-shell "0.4.1"]]

  :prep-tasks [["shell" "compile-editing"]
               ["shell" "compile-admin"]
               "javac" "compile"]

  :shell {:dir ".."
          :commands {"compile-editing" {:default-command "tools/editing.compile"}
                     "compile-admin"   {:default-command "tools/admin.compile"}}}

  :profiles {:dev {:dependencies [[midje "1.9.4"]
                                  [ring-mock "0.1.5"]
                                  [http-kit "2.3.0"]
                                  [org.postgresql/postgresql "42.2.5"]]

                   :resource-paths ["../reverie-sql/resources"]

                   :plugins [[lein-midje "3.1.3"]]}})
