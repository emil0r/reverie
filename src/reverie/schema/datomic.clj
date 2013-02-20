(ns reverie.schema.datomic
  (:use [datomic.api :only [q db] :as d]
        [reverie.core :only [reverie-object reverie-page]])
  (:import reverie.core.ObjectDatomic reverie.core.ReverieDataDatomic))


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

(defn- get-migrations [connection object]
  (q '[:find ?ks ?object :in $ ?object :where
       [?c :reverie.object.migrations/name ?object]
       [?c :reverie.object.migrations/keys ?ks]]
     (db connection) object))

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

(extend-type ObjectDatomic
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
  (object-upgrade [schema connection]
    (let [{:keys [object attributes ks]} (expand-schema schema)
          datomic-schema (vec (map :schema (map #(attributes %) ks)))
          migrations (get-migrations connection object)]
      @(d/transact connection [{:reverie.object.migrations/name object :db/id #db/id [:db.part/user -1]}
                               {:reverie.object.migrations/keys ks :db/id #db/id [:db.part/user -1]}])
      @(d/transact connection datomic-schema)))
  (object-synchronize [schema connection]
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
  (object-initiate [schema connection]
    (let [{:keys [object]} (expand-schema schema)
          initials (get-initials schema)
          idents (get-idents schema)
          tmpid {:db/id #db/id [:db.part/user -1]}
          attribs (apply conj [(merge tmpid {:reverie/object object})]
                         (into []
                               (map #(merge tmpid %)
                                    (cross-initials-idents initials idents))))
          tx @(d/transact connection attribs)]
      (assoc tx :db/id (-> tx :tempids vals last))))
  (object-get [schema connection id]
    (d/entity (db connection) id))
  (object-transform [schema entity]
    (let [idents (get-idents schema)]
      (into {} (map (fn [[attribute ident]] {attribute (get entity ident)}) idents))))
  (object-set [schema connection data id]
    (let [idents (get-idents schema)
          attribs (map (fn [[k attr]] {attr (data k)}) idents)]
      (let [attribs (into [] (map #(merge {:db/id id} %) attribs))]
        (-> @(d/transact connection attribs) (assoc :db/id id))))))


(extend-type ReverieDataDatomic
  reverie-page
  (page-render [rdata])
  (page-objects [rdata])
  (page-get-meta [rdata])
  (page-new-object [rdata object-data])
  (page-update-object [rdata object-data])
  (page-delete-object [rdata object-data])
  (page-new [{:keys [connection request] :as rdata}]
    (println connection request))
  (page-update [rdata])
  (page-delete [rdata])
  (page-restore [rdata])
  )
