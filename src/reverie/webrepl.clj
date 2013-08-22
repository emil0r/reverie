(ns reverie.webrepl
  (:require [cemerick.piggieback :as pig]
            [cljs.repl.browser :as browser]))

(defn init [] (pig/cljs-repl
               :repl-env (doto (browser/repl-env :port 9000)
                           (cljs.repl/-setup))))

;;(init)
