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
  `(let [~'func (fn [~'request ~@attributes] ~@body)]
     (into {} (map vector ~methods (repeat ~'func)))))

(macroexpand-1 (object-funcs [] [:get] (html5 request "body")))
(:get (object-funcs [] [:get] (html5 request)))
((:get (object-funcs [x y] [:get] (html5 request))) {:uri "/"} "my x" "my y")
x((:get (object-funcs [] [:get :post] (html5 [:div "my body"]) )) {})

(defmacro defobject [object options methods & args]
  (let [object (keyword object)
        options (parse-options options)
        `~body `(object-funcs [] ~methods ~@args)]
    `(do
       {~object {:options ~options
                 :fns ~body
                 }})))


(macroexpand-1 (defobject text [:areas [:a :b] :schema "asdf.clj"] [:get] "body"))
(macroexpand-1 (defobject text [:areas [:a :b] :schema "asdf.clj"] [:get] (html5 "body")))
(macroexpand-1 (defobject text [:areas [:a :b] :schema "asdf.clj"] [:any] "body"))
(macroexpand-1 (defobject text [:areas [:a :b] :schema "asdf.clj"] [:any] (html5 request "body")))


((:get (:fns (:text (defobject text [:areas [:a :b]] [:get] (html5 request "body"))))) {:uri "/"})
(-> (defobject text [:areas [:a :b] :schema "asdf.clj"] [:get] (html5 "body")) :text :fns :get)
