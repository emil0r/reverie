(ns reverie.core
  (:use [datomic.api :only [db tempid]]
        [clout.core]
        [slingshot.slingshot :only [try+ throw+]]))

(defonce routes (atom {}))
(defonce templates (atom {}))
(defonce objects (atom {}))
(defonce apps (atom {}))
(defonce plugins (atom {}))

(defprotocol reverie-object
  (object-correct? [schema]
    "Checks that the schema of an object is correct; checks for :schema, :initial and :input")
  (object-upgrade? [schema connection]
    "Does the object need an upgrade?")
  (object-upgrade! [schema connection]
    "Upgrades an object when a new schema has been added. Returns result of the upgrade")
  (object-synchronize! [schema connection]
    "Synchronizes all objects after an upgrade has been done")
  (object-initiate! [schema connection]
    "Initiate a newly created object. Returns the result + the id")
  (object-get [schema connection id]
    "Hashmap of all the attributes with associated values")
  (object-attr-transform [schema entity]
    "Returns a hashmap of the entity's attributes mapped to the attributes of the schema ")
  (object-set! [schema connection data id]
    "Set the attributes of an object")
  (object-render [schema connection id rdata]
    "Render an object"))

(defprotocol reverie-page
  (page-render [rdata]
    "Render the entire page. Return a data structure to pass onto ring.")
  (page-objects [rdata]
    "Returns a vector of all objects with the associated area/page")
  (page-get-meta [rdata]
    "Return all meta info about the page -> areas + template")
  (page-new-object! [rdata]
    "Add an object to the page")
  (page-update-object! [rdata]
    "Update an object")
  (page-delete-object! [rdata]
    "Delete an object from the page")
  (page-restore-object! [rdata]
    "Restore a deleted object")
  (page-new! [rdata]
    "Create a new page")
  (page-update! [rdata]
    "Update the page with the new data")
  (page-delete! [rdata]
    "Delete the page")
  (page-restore! [rdata]
    "Restore a deleted page")
  (page-get [rdata]
    "Get page")
  (pages-search [rdata]
    "Search pages for a match")
  (page-right? [rdata user right]
    "Does the user have that right for the page?"))

(defprotocol reverie-app
  (app-render [rdata]
    "Render the app. Return a data structure to pass onto ring."))

(defprotocol reverie-plugin
  (plugin-correct? [pdata])
  (plugin-upgrade? [pdata connection])
  (plugin-upgrade! [pdata connection])
  (plugin-get [pdata connection data])
  (plugin-set! [pdata connection data]))

(defrecord ObjectSchemaDatomic [object attributes])
(defrecord ReverieDataDatomic [])
(defrecord PluginDatomic [name options])

(defn reverie-data [data]
  (merge (ReverieDataDatomic.) data))

(defn get-plugin [name]
  (PluginDatomic. name (get @plugins name)))

(defn add-route! [route data]
  (swap! routes assoc route data))
(defn remove-route! [route]
  (swap! routes dissoc route))
(defn get-route [uri]
  (if-let [route-data (get @routes uri)]
    route-data
    (->>
     @routes
     (filter (fn [[k v]]
               (and
                (= (:type v) :app)
                (re-find (re-pattern k) uri))))
     first
     second)))

(defmulti parse-schema (fn [object options {:keys [schema]}] schema))
(defmethod parse-schema :default [object {:keys [attributes] :as options} settings]
  (ObjectSchemaDatomic.
   object
   (into {}
         (map (fn [m]
                (let [k (first
                         (filter
                          #(not (nil? (:db/ident (m %))))
                          (keys m)))
                      schema {:schema (merge (m k) {:db/id (tempid :db.part/db)
                                                    :db.install/_attribute :db.part/db})}]
                  {k (merge m schema)})) attributes))))

(defn- get-attributes [schema]
  (map #(-> % name symbol) (keys (:attributes schema))))

(defn start [connection]
  ;; TODO: implement, should run run-schemas!, start server and return
  ;; server. more?
  )

(defn- regex? [pattern]
  (= (class pattern) java.util.regex.Pattern))

(defn- get-schema [object]
  (:schema (get @objects (:reverie/object object))))

(defn run-schemas! [connection]
  (let [schemas (map #(:schema (@objects %)) (keys @objects))]
    (doseq [s schemas]
      (if (object-correct? s)
        (if (object-upgrade? s connection)
          (do
            (object-upgrade! s connection)
            (object-synchronize! s connection)))))))

(defn area-render [object rdata]
  (object-render (get-schema object)
                 (:connection rdata)
                 (:db/id object)
                 (assoc rdata :object-id (:db/id object))))

(defmacro area [name]
  (let [name (keyword name)]
    `(let [{:keys [~'mode]} ~'rdata]
       (if (= ~'mode :edit)
         [:div.reverie-area {:id ~name :name ~name :class ~name}
          (map #(area-render % ~'rdata) (page-objects (assoc ~'rdata :area ~name)))]
         (map #(area-render % ~'rdata) (page-objects (assoc ~'rdata :area ~name)))))))

(defn raise-response [response]
  (throw+ {:type :ring-response :response response}))

(defmacro defplugin [name options]
  (let [name (keyword name)]
    `(swap! plugins assoc ~name ~options )))


(defmacro deftemplate [template options & body]
  (let [template (keyword template)]
    `(swap! templates assoc ~template {:options ~options
                                       :fn (fn [~'rdata] (try+ ~@body
                                                              (catch [:type :ring-response] {:keys [~'response ~'type]}
                                                                ~'response)))})))


(defmacro object-funcs [attributes methods & body]
  (if (every? keyword? methods)
    `(let [~'func (fn [~'rdata {:keys [~@attributes]}] ~@body)]
       (into {} (map vector ~methods (repeat ~'func))))
    (let [paired (into {} (map (fn [[method fn-name]] {(keyword fn-name) method}) (partition 2 methods)))
          bodies (map (fn [[fn-name & fn-body]] [(keyword fn-name) fn-body]) (filter vector? body))]
      (loop [[func-vector & r] bodies
             m {}]
        (if (nil? func-vector)
          m
          (let [[fn-name fn-body] func-vector]
            (if-let [method (paired (first func-vector))]
              (recur r (assoc m method `(fn [~'rdata {:keys [~@attributes]}] ~@fn-body)))
              (recur r m))))))))

(defmacro defobject [object options methods & args]
   (let [object (keyword object)
         settings {}
         schema (parse-schema object options settings)
         attributes (get-attributes schema)
         body `(object-funcs ~attributes ~methods ~@args)]
     `(swap! objects assoc ~object (merge {:options ~options :schema ~schema} ~body))))

(defmacro app-method [[method options & body]]
  (case method
    :get (let [[route _2 _3] options
               regex (if (every? regex? (vals _2)) _2 nil)
               route (if (nil? regex)
                       (route-compile route)
                       (route-compile route regex))
               method-options (if (nil? regex) _2 _3)
               keys (vec (map #(-> % name symbol) (:keys route)))
               func `(fn [~'rdata {:keys ~keys}] (try+ ~@body
                                                      (catch [:type :ring-response] {:keys [~'response]}
                                                        ~'response)))]
           [method route method-options func])
    (let [[route _2 _3 _4] options
          [regex method-options form-data]
          (let [regex (if (and (map? _2) (every? regex? (vals _2))) _2 nil)]
            (case [(nil? regex) (nil? _3) (nil? _4)]
              [true true true] [regex nil _2]
              [true false true] [regex nil _3]
              [false false true] [regex nil _3]
              [regex _3 _4]))
          route (if (nil? regex)
                  (route-compile route)
                  (route-compile route regex))
          keys (vec (map #(-> % name symbol) (:keys route)))
          func `(fn [~'rdata {:keys ~keys} ~form-data] (try+ ~@body
                                                            (catch [:type :ring-response] {:keys [~'response]}
                                                              ~'response)))]
      ;; (println [(nil? regex) (nil? _3) (nil? _4)])
      ;; (println _2 _3 _4)
      ;; (println route regex method-options form-data)
      [method route method-options func]
      )))

(defmacro defapp [app options & methods]
  (let [app (keyword app)]
    (loop [[method & methods] methods
           fns []]
      (if (nil? method)
        `(swap! apps assoc ~app {:options ~options :fns ~fns})
        (recur methods (conj fns `(app-method ~method)))))))

