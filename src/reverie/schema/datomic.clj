(ns reverie.schema.datomic
  (:require [clojure.set :as set]
            [clout.core :as clout])
  (:use [datomic.api :only [q db] :as d]
        [reverie.core :only [reverie-object reverie-page reverie-module reverie-app
                             add-route! remove-route! get-route
                             templates objects] :as rev])
  (:import reverie.core.ObjectSchemaDatomic
           reverie.core.ReverieDataDatomic
           reverie.core.ModuleDatomic))


;; defaults are either values or functions that return a value

(defn- expand-schema [schema]
  {:object (:object schema)
   :attributes (:attributes schema)
   :ks (keys (:attributes schema))})

(defn- migrated? [ks migrations]
  (loop [[migration & migrations] (vec migrations)
         migrated? false]
    (if (nil? migration)
      migrated?
      (let [[mks] migration]
        (if (= mks ks)
          (recur migrations true)
          (recur migrations migrated?))))))

(defn- get-migrations [connection migration]
  (q '[:find ?ks ?migration :in $ ?migration :where
       [?c :reverie.migrations/name ?migration]
       [?c :reverie.migrations/keys ?ks]]
     (db connection) migration))

(defn- get-initials [schema]
  (let [{:keys [attributes ks]} (expand-schema schema)]
    (into {}  (map (fn [k] [k (-> (attributes k) :initial)]) ks))))

(defn- get-idents [schema]
  (let [{:keys [attributes ks]} (expand-schema schema)]
    (map (fn [k] [k (-> (attributes k) :schema :db/ident)]) ks)))

(defn- cross-initials-idents
  ([schema]
     (let [initials (get-initials schema)
           idents (get-idents schema)]
       (map (fn [[k attr]] {attr (initials k)})
            idents)))
  ([initials idents]
     (map (fn [[k attr]] {attr (initials k)})
          idents)))

(defn- clean-entity-map [e]
  (let [ks (keys e)]
    (assoc (select-keys e ks) :db/id (:db/id e))))

(defn- shorten-uri [request remove-part-of-uri]
  "shortens the uri by removing the unwanted part"
  (assoc-in request [:uri] (clojure.string/replace
                            (:uri request)
                            (re-pattern (clojure.string/replace remove-part-of-uri #"/$" "")) "")))

(defn- get-last-order [obj]
  (let [last-number (->> obj :reverie.page/_objects (map :reverie/order) sort last)]
    (if (nil? last-number)
      1
      (+ last-number 1))))

(extend-type ObjectSchemaDatomic
  reverie-object

  (object-correct? [schema]
    (let [{:keys [attributes ks]} (expand-schema schema)]
      (loop [[k & ks] ks
             correct? false]
        (if (nil? k)
          correct?
          (let [values (map #(get (attributes k) %) [:schema :initial :input :name])]
            (if (not-any? nil? values)
              (recur ks true)
              (recur ks correct?)))))))

  (object-upgrade? [schema connection]
    (let [{:keys [object ks]} (expand-schema schema)]
      (not (migrated? ks (get-migrations connection object)))))

  (object-upgrade! [schema connection]
    (let [{:keys [object attributes ks]} (expand-schema schema)
          datomic-schema (vec (map :schema (map #(attributes %) ks)))]
      @(d/transact connection [{:reverie.migrations/name object :db/id #db/id [:db.part/reverie -1]}
                               {:reverie.migrations/keys ks :db/id #db/id [:db.part/reverie -1]}])
      @(d/transact connection datomic-schema)))

  (object-synchronize! [schema connection]
    (let [{:keys [attributes ks]} (expand-schema schema)
          objects (map #(let [entity (d/entity (db connection) (first %))
                              ks (keys entity)]
                          [entity ks]) (q '[:find ?c :in $ :where [?c :reverie/object ?o]] (db connection)))
          ident-kws (->> (cross-initials-idents schema)
                         (map ffirst)
                         (cons :reverie/object)
                         sort)
          mismatched-objects (filter #(not (= ident-kws (sort (second %)))) objects)
          attribs (into {}  (cross-initials-idents schema))
          transactions (vec
                        (map (fn [[o i]]
                               (merge {:db/id (:db/id o)}
                                      (into {}
                                            (map (fn [k] {k (get attribs k)})
                                                 (clojure.set/difference
                                                  (set ident-kws)
                                                  (set i))))))
                             mismatched-objects))]
      @(d/transact connection transactions)))

  (object-initiate! [schema connection]
    (let [{:keys [object]} (expand-schema schema)
          initials (get-initials schema)
          idents (get-idents schema)
          attribs (merge {:db/id #db/id [:db.part/reverie -1]
                          :reverie/object object
                          :reverie/active? true}
                         (into {} (cross-initials-idents initials idents)))
          tx @(d/transact connection [attribs])]
      (assoc tx :db/id (-> tx :tempids vals last))))

  (object-move! [schema connection id {:keys [page-id area order]}]
    (let [obj (rev/object-get schema connection id)
          old-page (-> obj :reverie.page/_objects first)
          new-page (rev/page-get (rev/reverie-data {:connection connection
                                                    :page-id page-id}))
          order (cond order
                      :last (get-last-order obj)
                      :first 1
                      nil (get-last-order obj)
                      order)]
      (assoc
          {:page-id page-id
           :area area
           :order order}
        :tx @(d/transact connection
                         [;; update object with area
                          (merge (clean-entity-map obj) {:reverie/area area})
                          ;; remove object from old page
                          (assoc (clean-entity-map old-page)
                            :reverie.page/objects
                            (remove #(= (:db/id %) (:db/id obj))
                                    (:reverie.page/objects old-page)))
                          ;; add object to new page
                          (assoc (clean-entity-map new-page)
                            :reverie.page/objects
                            (conj (:reverie.page/objects new-page) (:db/id obj)))]))
      ))

  (object-copy! [schema connection id]
    (let [obj (rev/object-get schema connection id)
          tx @(d/transact connection
                          [(-> obj
                               (select-keys (keys obj))
                               (assoc :db/id (d/tempid :db.part/reverie))
                               (assoc :reverie/order (get-last-order obj)))])]
      (assoc tx :db/id (-> tx :tempids vals last))))

  (object-get [schema connection id]
    (d/entity (db connection) id))

  (object-attr-transform [schema entity]
    (let [idents (get-idents schema)]
      (into {} (map (fn [[attribute ident]] {attribute (get entity ident)}) idents))))
  
  (object-set! [schema connection id data]
    (let [idents (get-idents schema)
          attribs (remove #(-> % vals first nil?) ;; remove attribs with nil values
                          (map (fn [[k attr]] {attr (data k)}) idents))
          extra-data-ks (set/difference (-> data keys set)
                                        (->> idents (map first) set))]
      (let [attribs (merge (select-keys data extra-data-ks) (into {:db/id id} attribs))]
        (-> @(d/transact connection [attribs]) (assoc :db/id id)))))

  (object-publish! [schema connection id])

  (object-unpublish! [schema connection id])

  (object-render [schema connection id rdata]
    (let [object (rev/object-get schema connection id)
          request (:request rdata)]
      (if-let [func (or
                     (get (get @objects (:object schema))
                          (-> rdata :request :request-method))
                     (get (get @objects (:object schema)) :any))]
        (func rdata (rev/object-attr-transform schema object))))))


(extend-type ReverieDataDatomic
  reverie-page

  (page-render [{:keys [connection request] :as rdata}]
    (let [{:keys [uri]} request]
      (if-let [[route-uri page-data] (get-route uri)]
        (let [page (rev/page-get (assoc rdata :page-id (:page-id page-data)))]
          (case (:type page-data)
            :normal (let [template (get @templates (:reverie.page/template page))
                          fn (:fn template)]
                      (fn (assoc rdata :page-id (:db/id page))))
            :page (let [request (shorten-uri request route-uri)
                        [_ route _ func] (->> route-uri
                                              (get @rev/pages)
                                              :fns
                                              (filter #(let [[method route _ _] %]
                                                         (and
                                                          (= (:request-method request) method)
                                                          (clout/route-matches route request))))
                                              first)]
                    (if (nil? func)
                      {:status 404 :body "404, page not found"}
                      (func rdata (clout/route-matches route request))))
            (rev/app-render (assoc rdata :page-data page-data :page page)))))))

  (page-objects [{:keys [connection page-id area] :as rdata}]
    (let [page (d/entity (db connection) page-id)]
      (sort-by :reverie/order
               (filter #(and
                         (:reverie/active? %)
                         (= (:reverie/area %) area)) (:reverie.page/objects page)))))
  
  (page-get-meta [rdata])
  
  (page-new-object! [{:keys [connection object-id page-id] :as rdata}]
    (let [page (d/entity (db connection) page-id)
          tx @(d/transact connection
                          [{:db/id page-id
                            :reverie.page/objects object-id}])]
      (assoc rdata :tx tx)))
  
  (page-update-object! [rdata]) ;; datomic allows upside travseral?

  (page-delete-object! [{:keys [connection object-id] :as rdata}]
    (let [tx @(d/transact connection
                          [{:db/id object-id
                            :reverie/active? false}])]
      (assoc rdata :tx tx)))

  (page-new! [{:keys [connection parent tx-data page-type] :as rdata}]
    (let [uri (:reverie.page/uri tx-data)
          tx @(d/transact connection
                          [(merge tx-data
                                  {:db/id #db/id [:db.part/reverie]
                                   :reverie/active? true
                                   :reverie.page/objects []})])
          page-id (-> tx :tempids vals last)]
      (add-route! uri {:page-id page-id :type (or page-type :normal)
                       :template (:reverie.page/template tx-data)})
      (merge rdata {:tx tx :page-id page-id})))

  (page-update! [{:keys [connection page-id tx-data] :as rdata}]
    (let [old-uri (:reverie.page/uri (rev/page-get rdata))
          new-uri (:reverie.page/uri tx-data)
          tx @(d/transact connection
                          [(merge tx-data {:db/id page-id})])]
      (if (and
           (not (nil? new-uri))
           (not= new-uri old-uri))
        (rev/update-route! new-uri (rev/get-route old-uri)))
      (assoc rdata :tx tx)))

  (page-delete! [{:keys [connection page-id] :as rdata}]
    (let [uri (:page.reverie/uri (rev/page-get rdata))
          tx @(d/transact connection
                          [{:db/id page-id
                            :reverie/active? false}])]
      (rev/remove-route! uri)
      (assoc rdata :tx tx)))

  (page-restore! [{:keys [connection page-id] :as rdata}]
    (let [tx @(d/transact connection
                          [{:db/id page-id
                            :reverie/active? true}])]
      (assoc rdata :tx tx)))

  (page-get [{:keys [connection page-id] :as rdata}]
    (d/entity (db connection) page-id))

  (page-publish! [rdata])

  (page-unpublish! [rdata])

  (page-rights? [rdata user right]))

(extend-type ReverieDataDatomic
  reverie-app
  (app-render [{:keys [connection request page] :as rdata}]
    (if-let [app (@rev/apps (:reverie.page/app page))]
      (let [request (shorten-uri request (:reverie.page/uri page))
            [_ route _ func] (->> app
                                  :fns
                                  (filter #(let [[method route _ _] %]
                                             (and
                                              (= (:request-method request) method)
                                              (clout/route-matches route request))))
                                  first)]
        (if (nil? func)
          {:status 404 :body "404, page not found"}
          (func rdata (clout/route-matches route request)))))))

(defn- valid-module-schema? [schema]
  (let [needed [:db/ident :db/valueType :db/cardinality :db/doc]]
    (loop [[s & r] schema
           valid? true]
      (if (nil? s)
        valid?
        (recur r (every? #(-> (get s %) nil? not) needed))))))

(extend-type ModuleDatomic
  reverie-module

  (module-correct? [pdata]
    (let [schema (-> pdata :options :schema)]
      (and
       (not (nil? schema))
       (valid-module-schema? schema))))

  (module-upgrade? [pdata connection]
    (let [ks (-> pdata :options :schema keys)]
      (not (migrated? ks (get-migrations connection (:name pdata))))))

  (module-upgrade! [pdata connection]
    (let [schema (-> pdata :options :schema)
          ks (map #(:db/ident %) schema)]
      @(d/transact connection [{:reverie.migrations/name (:name pdata) :db/id #db/id [:db.part/reverie -1]}
                               {:reverie.migrations/keys ks :db/id #db/id [:db.part/reverie -1]}])
      @(d/transact connection (map #(merge % {:db/id (d/tempid :db.part/db)
                                              :db.install/_attribute :db.part/db}) schema))))

  (module-get [pdata connection data]
    )
  
  (module-set! [pdata connection data]
    ))
