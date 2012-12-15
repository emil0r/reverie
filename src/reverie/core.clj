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
               (recur r (assoc m method `(fn [~'request] ~@fn-body)))
               (recur r m)))))))))

(macroexpand-1 (object-funcs [] [:get] (html5 request "body")))
(macroexpand-1 (object-funcs [] [:get fn-get :post fn-post] [fn-get "get function"] [fn-post "post function"]))
((:post (object-funcs [] [:get fn-get :post fn-post] [fn-get (html5 "get function" "yeah?" request)] [fn-post "post function"])) {:uri "/"})
(:get (object-funcs [] [:get] (html5 request)))
((:get (object-funcs [x y] [:get] (html5 request x y))) {:uri "/"} "my x" "my y")
((:get (object-funcs [] [:get :post] (html5 [:div "my body"]) )) {})

(defmacro defobject [object options methods & args]
  (let [object (keyword object)
        options (parse-options options)
        `~body `(object-funcs [] ~methods ~@args)]
    `{~object (merge {:options ~options} ~body)}))


(macroexpand-1 (defobject text [:areas [:a :b] :schema "asdf.clj"] [:get] "body"))
(macroexpand-1 (defobject text [:areas [:a :b] :schema "asdf.clj"] [:get] (html5 "body")))
(macroexpand-1 (defobject text [:areas [:a :b] :schema "asdf.clj"] [:any] "body"))
(macroexpand-1 (defobject text [:areas [:a :b] :schema "asdf.clj"] [:any] (html5 request "body")))

(:text (defobject text [:areas [:a :b]] [:get] (html5 request "body")))
((:get (:text (defobject text [:areas [:a :b]] [:get] (html5 request "body")))) {:uri "/"})
(-> (defobject text [:areas [:a :b] :schema "asdf.clj"] [:get] (html5 "body")) :text :get)
