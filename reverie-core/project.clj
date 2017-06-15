(defproject reverie-core "0.9.0-SNAPSHOT"
  :description "The core of reverie; a CMS for power users"
  :url "http://reveriecms.org"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [;; core
                 [org.clojure/clojure "1.9.0-alpha16"]
                 [org.clojure/core.match "0.2.2"]
                 [org.clojure/core.async "0.3.442"]
                 [org.clojure/core.memoize "0.5.9"]

                 ;; core.async fails to compile unless more up to date
                 ;; version of tools.analyzer and tools.analyzer.jvm is
                 ;; present
                 [org.clojure/tools.analyzer "0.6.9"]
                 [org.clojure/tools.analyzer.jvm "0.7.0"]

                 ;; structure
                 [com.stuartsierra/component "0.3.2"]

                 ;; web
                 [ring/ring-anti-forgery "1.0.1"]
                 [ring/ring-core "1.5.1"]
                 [clout "2.1.2"]
                 [hiccup "1.0.5"]
                 [vlad "3.3.2"]
                 [lib-noir "0.9.9"]
                 [ez-web "0.3.0"]
                 [ez-image "1.0.4"]

                 ;; time
                 [clj-time "0.13.0"]

                 ;; email
                 [ez-email "0.1.0"]

                 ;; reverie specific
                 [bultitude "0.2.8"]

                 ;; exception handling
                 [slingshot "0.12.2"]

                 ;; hashing, digests, crypto, etc
                 [buddy "1.3.0"]
                 [digest "1.4.5"]

                 ;; logging
                 [com.taoensso/timbre "4.7.4"]

                 ;; i18n
                 [com.taoensso/tower "3.0.2"]

                 ;; filesystem
                 [me.raynes/fs "1.4.6"]

                 ;; scheduling
                 [im.chit/cronj "1.4.3"]

                 ;; schema
                 [prismatic/schema "1.1.0"]]
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
                     "compile-admin" {:default-command "tools/admin.compile"}}}
  :profiles {:dev {:dependencies [[midje "1.9.0-alpha6"]
                                  [ring-mock "0.1.5"]
                                  [spyscope "0.1.7-SNAPSHOT"]
                                  [http-kit "2.2.0"]
                                  ;;[org.clojure/tools.namespace "0.2.10"]
                                  [org.postgresql/postgresql "42.0.0"]]
                   :injections [(require 'spyscope.core)
                                (require 'spyscope.repl)
                                (require '[clojure.tools.namespace.repl :refer [refresh]])]
                   :resource-paths ["../reverie-sql/resources"]
                   :plugins [[lein-midje "3.1.3"]]}})
