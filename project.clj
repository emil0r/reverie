(defproject reverie "0.1.0-SNAPSHOT"
  :description "A sane CMS"
  :url "http://reveriecms.org"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [ring "1.2.0"]
                 [korma "0.3.0-RC5"]
                 [me.raynes/fs "1.4.5"]
                 [clout "1.1.0"]
                 [slingshot "0.10.3"]
                 [bultitude "0.1.5"]
                 [lobos "1.0.0-beta1"]
                 [lib-noir "0.6.6"]]
  :ring {:handler reveriecms.dev/app}
  :profiles {:dev {:dependencies [[midje "1.6-alpha1"]
                                  [com.stuartsierra/lazytest "1.2.3"]
                                  [ring-mock "0.1.3"]
                                  [org.postgresql/postgresql "9.2-1002-jdbc4"]
                                  [cljsbuild "0.3.2"]
                                  [org.clojure/tools.trace "0.7.5"]]}}
  :cljsbuild {:builds [{:source-paths ["src-cljs"]
                        :compiler {:output-to "resources/public/admin/js/main.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}]}
  :repositories {"stuart" "http://stuartsierra.com/maven2"})
