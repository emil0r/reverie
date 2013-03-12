(ns reverie.core
  (:use [datomic.api :only [db]]))

(defonce routes (atom {}))
(defonce templates (atom {}))
(defonce objects (atom {}))
(defonce apps (atom {}))

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
    "Set the attributes of an object"))

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

(defrecord ObjectSchemaDatomic [object attributes])
(defrecord ReverieDataDatomic [])

(defn reverie-data [data]
  (merge (ReverieDataDatomic.) data))

(defn- parse-options [options]
  (loop [m {}
         [opt & options] (partition 2 options)]
    (if (nil? opt)
      m
      (let [[k v] opt]
        (recur (assoc m k v) options)))))

(defmulti parse-schemas (fn [object options {:keys [schema]}] schema))
(defmethod parse-schemas :default [object {:keys [attributes] :as options} settings]
  (let [schemas-data (into {}
                           (map (fn [m]
                                  (let [k (first
                                           (filter
                                            #(not (nil? (:db/ident (m %))))
                                            (keys m)))
                                        schema {:schema (merge (m k) {:db/id #db/id [:db.part/db]
                                                                      :db.install/_attribute :db.part/db})}]
                                    {k (merge m schema)})) attributes))]
    (ObjectSchemaDatomic. object schemas-data)))

(defn- get-attributes [schemas]
  (map #(-> % name symbol) (keys (:attributes schemas))))

(defn run-schemas! [connection]
  (let [schemas (map #(:schemas (@objects %)) (keys @objects))]
    (doseq [s schemas]
      (if (object-correct? s)
        (if (object-upgrade? s connection)
          (do
            (object-upgrade! s connection)
            (object-synchronize! s connection)))))))

(defmacro area [name]
  (let [name (keyword name)]
    `(let [{:keys [~'mode]} ~'rdata]
       (if (= ~'mode :edit)
         [:div.reverie-area {:id ~name :name ~name :class ~name}
          (page-objects (assoc ~'rdata :area ~name))]
         (page-objects (assoc ~'rdata :area ~name))))))

(defmacro deftemplate [template options & body]
  (let [template (keyword template)
        options (parse-options options)]
    `(swap! routes assoc ~template {:options ~options
                                    :fn (fn [~'rdata] ~@body)})))

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
         settings {}
         schemas (parse-schemas object options settings)
         attributes (get-attributes schemas)
         body `(object-funcs ~attributes ~methods ~@args)]
     `(swap! objects assoc ~object (merge {:options ~options :schemas ~schemas} ~body))))
