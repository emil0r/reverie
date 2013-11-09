(defproject reverie "0.6.0-SNAPSHOT"
  :description "A friendly CMS for the web"
  :url "http://reveriecms.org"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [ring "1.2.0"]
                 ;;[ring-anti-forgery "0.2.1"]
                 [ring/ring-json "0.2.0"]
                 [clj-time "0.6.0"]
                 [korma "0.3.0-RC5"]
                 [me.raynes/fs "1.4.5"]
                 
                 ;; DO NOT WANT
                 [org.clojars.emil0r/hiccup "1.0.3a"]
                 [org.clojars.emil0r/lobos "1.0.0-beta2"]
                 ;; DO NOT WANT
                 
                 [org.clojure/core.async "0.1.242.0-44b1e3-alpha"]
                 [ez-image "1.0"]
                 [clout "1.1.0"]
                 [slingshot "0.10.3"]
                 [bultitude "0.1.7"]
                 [lib-noir "0.6.6"]]
  :ring {:init reveriedev.dev/init
         :handler reveriedev.dev/app}
  :profiles {:dev {:dependencies [[midje "1.6-alpha1"]
                                  [com.stuartsierra/lazytest "1.2.3"]
                                  [ring-mock "0.1.3"]
                                  [org.postgresql/postgresql "9.2-1002-jdbc4"]
                                  [crate "0.2.4"]
                                  [jayq "2.4.0"]
                                  [cljsbuild "0.3.3"]
                                  [org.clojure/clojurescript "0.0-1853" :exclusions [[org.apache.ant/ant]]]
                                  [org.clojure/google-closure-library-third-party "0.0-2029"]
                                  [org.clojure/tools.trace "0.7.5"]
                                  [com.cemerick/piggieback "0.1.0"]]
                   :plugins [[lein-cljsbuild "0.3.3"]]}}
  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  :cljsbuild {:builds {:prod {:source-paths ["src-cljs"]
                              :compiler {:output-to "resources/public/admin/js/main.js"
                                         :optimizations :advanced
                                         :pretty-print false
                                         :externs ["externs/jquery-1.8.js"
                                                   "externs/dynatree.js"]}}
                       :prod-simple {:source-paths ["src-cljs"]
                                     :compiler {:output-to "resources/public/admin/js/main-simple.js"
                                                :optimizations :simple
                                                :pretty-print false}}
                       :dev {:source-paths ["src-cljs"]
                             :compiler {:output-to "resources/public/admin/js/main-dev.js"
                                        :optimizations :whitespace
                                        :pretty-print true}}}}
  :repositories {"stuart" "http://stuartsierra.com/maven2"
                 "sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"})
