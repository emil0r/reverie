(ns reverie.schema.datomic
  (:use [datomic.api :only [q db] :as d]
        [reverie.core :only [reverie-schema]]))


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
  (d/q '[:find ?ks :in $ ?object :where
         [?c :reverie.object.migrations/name ?object]
         [?c :reverie.object.migrations/keys ?ks]]
       (db connection) object))

(extend-type SchemaDatomic
  reverie-schema
  (schema-initiate [schema connection]
    (let [object (:object schema)
          attributes (:attributes schema)
          ks (keys attributes)
          datomic-schema (vec (map :schema (map #(attributes %) ks)))
          idents (map :db/ident datomic-schema)
          migrations (get-migrations connection object)]
      @(d/transact connection
                   (apply conj datomic-schema
                          [{:reverie.object.migrations/name object :db/id #db/id [:db.part/user -1]}
                           {:reverie.object.migrations/keys ks :db/id #db/id [:db.part/user]}]))))
  (schema-correct? [schema]
    (let [attributes (:attributes schema)
          ks (keys attributes)]
      (loop [[k & ks] ks
             correct? true]
        (if (nil? k)
          correct?
          (let [values (map #(get (attributes k) %) [:schema :initial :input])]
            (if (not-any? nil? values)
              (recur ks correct?)
              (recur ks false)))))))
  (schema-upgrade? [schema connection])
  (schema-upgrade [schema connection])
  (schema-get [schema connection])
  (schema-set [schema connection data]))

