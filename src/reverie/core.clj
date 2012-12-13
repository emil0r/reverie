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

(defmacro testus [& args]
  (let [fn-name (keyword (first args))]
    `(let [~'func (fn [~'x ~'y ~'request] ~(last args))]
       {~fn-name ~'func})))

((:test (testus test (println (html5 x y request)))) "my x" "my y" {})
(macroexpand-1 (testus test (println x y request)))

(defmacro deftemplate [template options body]
  (let [template (keyword template)
        options (parse-options options)]
    `(swap! routes assoc ~template {:options ~options
                                    :fn (fn [~'request] ~body)})))


(defmacro object-funcs [schema args]
  (let [kw? (keyword? (first args))
        methods (-> args butlast vec)]
   `(if ~kw?
      (let [~'func (fn [~'request] ~(last args))]
         (into {} (map vector ~methods (repeat ~'func))))
       (let [~'func (fn [~'request] ~(last args))]
         (into {} (map vector [:get :post] (repeat ~'func)))))))

(macroexpand-1 (object-funcs [] [:get (html5 request "body")]))
(:get (object-funcs [] [:get (html5 request)]))
((:get (object-funcs [] (list :get (html5 request)))) {:uri "/"})
((:get (object-funcs [] (list :get :post (html5 [:div "my body"])) )) {})

(defmacro defobject [object options & args]
  (let [object (keyword object)
        options (parse-options options)
        `~body `(object-funcs [] ~args)]
    `(do
       {~object {:options ~options
                 :fns ~body
                 }})))


(macroexpand-1 (defobject text [:areas [:a :b] :schema "asdf.clj"] :get "body"))
(macroexpand-1 (defobject text [:areas [:a :b] :schema "asdf.clj"] :get (html5 "body")))
(macroexpand-1 (defobject text [:areas [:a :b] :schema "asdf.clj"] "body"))
(macroexpand-1 (defobject text [:areas [:a :b] :schema "asdf.clj"] (html5 request "body")))


((:get (:fns (:text (defobject text [:areas [:a :b]] :get (html5 request "body"))))) {:uri "/"})
(-> (defobject text [:areas [:a :b] :schema "asdf.clj"] :get (html5 "body")) :text :fns :get)
