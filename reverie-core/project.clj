(defproject reverie-core "0.8.0-alpha7"
  :description "The core of reverie; a CMS for power users"
  :url "http://reveriecms.org"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [;; core
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/core.match "0.2.2"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [org.clojure/core.memoize "0.5.8"]

                 ;; structure
                 [com.stuartsierra/component "0.2.3"]

                 ;; web
                 [ring/ring-anti-forgery "1.0.0"]
                 [ring/ring-core "1.4.0"]
                 [clout "2.1.2"]
                 [hiccup "1.0.5"]
                 [vlad "3.3.0"]
                 [lib-noir "0.9.9"]
                 [ez-web "0.3.0"]
                 [ez-image "1.0.4"]

                 ;; time
                 [clj-time "0.11.0"]

                 ;; email
                 [ez-email "0.1.0"]

                 ;; reverie specific
                 [bultitude "0.2.8"]

                 ;;
                 [slingshot "0.12.2"]

                 ;; hashing, digests, crypto, etc
                 [buddy/buddy-core "0.9.0"]
                 [buddy/buddy-hashers "0.11.0"]
                 [buddy/buddy-auth "0.9.0"]
                 [digest "1.4.4"]

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
  :profiles {:dev {:dependencies [[midje "1.6.3"]
                                  [ring-mock "0.1.5"]
                                  [spyscope "0.1.5"]
                                  [http-kit "2.1.19"]
                                  [org.clojure/tools.namespace "0.2.11"]
                                  [org.postgresql/postgresql "9.3-1102-jdbc41"]]
                   :injections [(require 'spyscope.core)
                                (require 'spyscope.repl)
                                (require '[clojure.tools.namespace.repl :refer [refresh]])]
                   :resource-paths ["../reverie-sql/resources"]
                   :plugins [[lein-midje "3.1.3"]]}})
