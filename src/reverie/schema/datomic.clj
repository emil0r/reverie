(ns reverie.schema.datomic
  (:use [datomic.api :only [q db] :as d]
        [reverie.core :only [reverie-object]]))


;; defaults are either values or functions that return a value
(defrecord SchemaDatomic [object attributes])

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
  (d/q '[:find ?ks ?object :in $ ?object :where
         [?c :reverie.object.migrations/name ?object]
         [?c :reverie.object.migrations/keys ?ks]]
       (db connection) object))

(defn- expand-schema [schema]
  {:object (:object schema)
   :attributes (:attributes schema)
   :ks (keys (:attributes schema))})

(extend-type SchemaDatomic
  reverie-object
  (object-correct? [schema]
    (let [{:keys [attributes ks]} (expand-schema schema)]
      (loop [[k & ks] ks
             correct? true]
        (if (nil? k)
          correct?
          (let [values (map #(get (attributes k) %) [:schema :initial :input])]
            (if (not-any? nil? values)
              (recur ks correct?)
              (recur ks false)))))))
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
  (object-get [schema connection id]
    (let [{:keys [attributes ks]} (expand-schema schema)]
      ))
  (object-set [schema connection data id]
    (let [{:keys [attributes ks]} (expand-schema schema)
          idents (map (fn [k] [k (-> (attributes k) :schema :db/ident)]) ks)
          attribs (map (fn [[k attr]] {attr (data k)}) idents)]
      (if (nil? id)
        (let [attribs (into [] (map #(merge {:db/id #db/id [:db.part/user] } %) attribs))
              tx @(d/transact connection attribs)]
          (assoc tx :db/id (-> tx :tempids vals last)))
        (let [attribs (into [] (map #(merge {:db/id id} %) attribs))]
          (-> @(d/transact connection attribs) (assoc :db/id id)))))))

