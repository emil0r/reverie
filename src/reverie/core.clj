(ns reverie.core)

(defonce routes (atom {}))
(defonce templates (atom {}))
(defonce objects (atom {}))
(defonce apps (atom {}))


(defn html5 [& args]
  (println "html5 hit")
  (str "args->" args))

(def request-methods [:get :post :put :delete :options :head])

(defn- parse-options [options]
  (loop [m {}
         [opt & options] (partition 2 options)]
    (if (nil? opt)
      m
      (let [[k v] opt]
        (recur (assoc m k v) options)))))

(defmacro deftemplate [template options body]
  (let [template (keyword template)
        options (parse-options options)]
    `(swap! routes assoc ~template {:options ~options
                                    :fn (fn [~'request] ~body)})))

(defmacro object-funcs [attributes methods & body]
  (let [all-kw? (zero? (count (filter #(not (keyword? %)) methods)))]
    (if all-kw?
      `(let [~'func (fn [~'request ~@attributes] ~@body)]
         (into {} (map vector ~methods (repeat ~'func))))
      (let [paired (into {} (map (fn [[method fn-name]] {(keyword fn-name) method }) (partition 2 methods)))
            bodies (map (fn [[fn-name & fn-body]] [(keyword fn-name) fn-body]) (filter vector? body))]
        (loop [[func-vector & r] bodies
               m {}]
          (if (nil? func-vector)
            m
            (let [[fn-name fn-body] func-vector]
             (if-let [method (paired (first func-vector))]
               (recur r (assoc m method `(fn [~'request ~@attributes] ~@fn-body)))
               (recur r m)))))))))

(defmacro defobject [object options methods & args]
  (let [object (keyword object)
        options (parse-options options)
        `~body `(object-funcs [] ~methods ~@args)]
    `{~object (merge {:options ~options} ~body)}))
