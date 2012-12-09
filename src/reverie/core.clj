(ns reverie.core)

(defonce routes (atom {}))
(defonce templates (atom {}))
(defonce objects (atom {}))
(defonce apps (atom {}))



(defmacro deftemplate [template args body]
  (let [template (keyword template)
        options (loop [m {}
                       [opt & options] (partition 2 args)]
                  (if (nil? opt)
                    m
                    (let [[k v] opt]
                      (recur (assoc m k v) options))))]
    `(swap! routes assoc ~template {:options ~options
                                    :fn (fn [~'request] ~body)})))

