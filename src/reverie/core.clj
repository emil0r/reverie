(ns reverie.core
  (:require [korma.core :as korma])
  (:use [datomic.api :only [db tempid]]
        [clout.core]
        [slingshot.slingshot :only [try+ throw+]]))

(defonce apps (atom {}))
(defonce modules (atom {}))
(defonce objects (atom {}))
(defonce pages (atom {}))
(defonce routes (atom {}))
(defonce settings (atom {}))
(defonce templates (atom {}))

(korma/defentity role)
(korma/defentity page
  (korma/many-to-many role :role_page))
(korma/defentity page_attributes
  (korma/belongs-to page))
(korma/defentity object
  (korma/belongs-to page))
(korma/defentity app
  (korma/belongs-to page))
(korma/defentity user
  (korma/many-to-many role :role_user))
(korma/defentity group
  (korma/many-to-many role :role_group))

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
  (object-move! [schema connection id data]
    "Move an object between pages and areas")
  (object-copy! [schema connection id]
    "Copy an object")
  (object-get [schema connection id]
    "Hashmap of all the attributes with associated values")
  (object-attr-transform [schema entity]
    "Returns a hashmap of the entity's attributes mapped to the attributes of the schema ")
  (object-set! [schema connection id data]
    "Set the attributes of an object")
  (object-publish! [schema connection id]
    "Publish an object")
  (object-unpublish! [schema connection id]
    "Unpublish an object")
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
  (page-publish! [rdata]
    "Publish a page")
  (page-unpublish! [rdata]
    "Unpublish a page")
  (page-get [rdata]
    "Get page")
  (pages-search [rdata]
    "Search pages for a match")
  (page-rights? [rdata user right]
    "Does the user have that right for the page?"))

(defprotocol reverie-app
  (app-render [rdata]
    "Render the app. Return a data structure to pass onto ring."))

(defprotocol reverie-module
  (module-correct? [pdata])
  (module-upgrade? [pdata connection])
  (module-upgrade! [pdata connection])
  (module-get [pdata connection data])
  (module-set! [pdata connection data]))

(defrecord ObjectSchemaDatomic [object attributes])
(defrecord ReverieDataDatomic [])
(defrecord ModuleDatomic [name options])

(defn reverie-data [data]
  (merge (ReverieDataDatomic.) data))

(defn get-module [name]
  (ModuleDatomic. name (get @modules name)))

(defn add-route! [uri route]
  (swap! routes assoc uri route))
(defn remove-route! [uri]
  (swap! routes dissoc uri))
(defn update-route! [new-uri {:keys [uri] :as route}]
  (remove-route! uri)
  (add-route! new-uri route))
(defn get-route [uri]
  (if-let [route-data (get @routes uri)]
    [uri route-data]
    (->>
     @routes
     (filter (fn [[k v]]
               (and
                (not= (:type v) :normal)
                (re-find (re-pattern k) uri))))
     first)))

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

(defn- get-attributes [options]
  (map symbol (map name (keys (:attributes options)))))

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

(defmacro defmodule [name options]
  (let [name (keyword name)]
    `(swap! modules assoc ~name ~options )))


(defmacro deftemplate [template options & body]
  (let [template (keyword template)]
    `(swap! templates assoc ~template {:options ~options
                                       :fn (fn [~'rdata] (try+ {:status 200
                                                               :headers (or (:headers ~options) {})
                                                               :body ~@body}
                                                              (catch [:type :ring-response] {:keys [~'response ~'type]}
                                                                ~'response)))})))


(defmacro object-funcs [attributes methods & body]
  (if (every? keyword? methods)
    `(let [~'func (fn [~'data {:keys [~@attributes]}] ~@body)]
       (into {} (map vector ~methods (repeat ~'func))))
    (let [paired (into {} (map (fn [[method fn-name]] {(keyword fn-name) method}) (partition 2 methods)))
          bodies (map (fn [[fn-name & fn-body]] [(keyword fn-name) fn-body]) (filter vector? body))]
      (loop [[func-vector & r] bodies
             m {}]
        (if (nil? func-vector)
          m
          (let [[fn-name fn-body] func-vector]
            (if-let [method (paired (first func-vector))]
              (recur r (assoc m method `(fn [~'data {:keys [~@attributes]}] ~@fn-body)))
              (recur r m))))))))

(defmacro defobject [object options methods & args]
   (let [object (keyword object)
         settings {}
         attributes (get-attributes options)
         table-symbol (or (:table options) object)
         body `(object-funcs ~attributes ~methods ~@args)]
     `(korma/defentity (name ~object)
        (korma/table ~table-symbol)
        (korma/belongs-to object))
     `(swap! objects assoc ~object (merge {:options ~options
                                           :entity ~object} ~body))))

(defmacro request-method
  "Pick apart the request methods specified in other macros"
  [[method options & body]]
  (case method
    :get (let [[route _2 _3] options
               regex (if (every? regex? (vals _2)) _2 nil)
               route (if (nil? regex)
                       (route-compile route)
                       (route-compile route regex))
               method-options (if (nil? regex) _2 _3)
               keys (vec (map #(-> % name symbol) (:keys route)))
               func `(fn [~'data {:keys ~keys}] (try+ {:status 200
                                                       :headers (or (:headers ~method-options) {})
                                                       :body ~@body}
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
          func `(fn [~'data {:keys ~keys} ~form-data] (try+ {:status 200
                                                             :headers (or (:headers ~method-options) {})
                                                             :body ~@body}
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
        (recur methods (conj fns `(request-method ~method)))))))


(defmacro defpage [path options & methods]
  (loop [[method & methods] methods
         fns []]
    (if (nil? method)
      (do
        (add-route! path {:type :page})
        `(swap! pages assoc ~path {:options ~options :fns ~fns}))
      (recur methods (conj fns `(request-method ~method))))))

(defn get-connection []
  (db (get @settings :connection-string)))
