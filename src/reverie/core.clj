(ns reverie.core)

(defonce routes (atom {}))
(defonce templates (atom {}))
(defonce objects (atom {}))
(defonce apps (atom {}))


(defn html5 [& args]
  (println "html5 hit")
  (str "args->" args))


(def attributes {:text {:schema {} :initial "" :input :text}
                 :image {:schema {} :initial "" :input :image}
                 :city {:schema {}
                        :input :select
                        :initial 0
                        :options (fn [] {0 "" 1 "Stockholm" 2 "Harare" 3 "Oslo"})}})


;; checks that the object has the schema correctly set up
;; checks if the object needs to be upgraded
;; upgrades the object
;; initiates the object with the initial data
;; gets the data from the object
;; sets the data to the object
(defprotocol reverie-object
  (object-correct? [schema]) ;; true or false
  (object-upgrade? [schema connection]) ;; true or false
  (object-upgrade [schema connection]) ;; returns result
  (object-initiate [schema connection id]) ;; returns result
  (object-get [schema connection id]) ;; hashmap of all the attributes with associated values
  (object-set [schema connection data id])) ;; set the attributes


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
      (let [paired (into {} (map (fn [[method fn-name]] {(keyword fn-name) method}) (partition 2 methods)))
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
    `(swap! objects assoc ~object (merge {:options ~options} ~body))))
