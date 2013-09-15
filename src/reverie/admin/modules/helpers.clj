(ns reverie.admin.modules.helpers
  (:require [clojure.string :as s]
            [korma.core :as k])
  (:use [reverie.atoms :only [modules]]))

(defn get-module [{:keys [real-uri]}]
  (let [name (-> real-uri
                 (s/replace #"/admin/frame/module/" "")
                 (s/split #"/")
                 first
                 keyword)]
    {:module-name name
     :module (@modules name)}))

(defn get-entity-name [module entity]
  (or (get-in module [:entities (keyword entity) :name])
      (s/capitalize (name entity))))

(defn get-display-fields [module entity]
  (let [e (get-in module [:entities (keyword entity)])]
    (or (:display e)
        (sort (keys (remove (fn [[_ x]] (= (:type x) :m2m)) (:fields e)))))))

(defn get-field-name
  ([field data]
     (or (:name data)
         (s/capitalize (name field))))
  ([module entity field]
     (or (get-in module [:entities (keyword entity) :fields (keyword field) :name])
         (s/capitalize (name field)))))

(defmulti get-fields (fn [what & _] (cond
                                    (not (nil? (:entities what))) :module
                                    :else :entity)))
(defmethod get-fields :module
  ([module entity]
     (into
      {}
      (remove
       (fn [[_ d]] (some #(= (:type d) %) [:m2m]))
       (get-in module [:entities (keyword entity) :fields]))))
  ([module entity fields]
     (into
      {}
      (filter (fn [[f _]]
                (some #(= f %) fields))
              (get-fields (keyword module) entity)))))
(defmethod get-fields :entity
  ([entity]
     (:fields entity))
  ([entity fields]
     (into
      {}
      (filter (fn [[f _]]
                (some #(= f %) fields))
              (get-fields entity)))))

(defn get-field [module entity field]
  (get-in module [:entities (keyword entity) :fields field]))

(defn get-entities [module entity page]
  (let [order (get-in module [:entities (keyword entity) :order])
        offset (if page
                 (* 50 (- (read-string page) 1))
                 0)]
    (if order
      (k/select entity
                (k/limit 50)
                (k/offset offset)
                (k/order order))
      (k/select entity
                (k/limit 50)
                (k/offset offset)))))

(defn get-entity-row [entity display-fields m-name e-name]
  [:tr
   [:td [:a {:href (str "/admin/frame/module/"
                        (name m-name)
                        "/"
                        (name e-name)
                        "/" (:id entity))} (get entity (first display-fields))]]
   (map (fn [field] [:td (get entity field)]) (rest display-fields))])

(defn get-instance-name [module entity id]
  (if-let [display (first (get-display-fields module entity))]
    (get (first (k/select entity (k/where {:id id}))) display)
    id))



(defn pre-process-data [data module entity]
  (if-let [pre (get-in module [:entities (keyword entity) :pre])]
    (pre data)
    data))

(defn post-process-data [data module entity]
  (if-let [post (get-in module [:entities (keyword entity) :post])]
    (post data)
    data))

(defn get-field-attribs [data]
  (reduce (fn [out k]
            (if (nil? out)
              out
              (if (k data)
                (assoc out k (k data))
                out)))
          {}
          [:max]))

(defn drop-down-m2m-data [module entity m2m entity-id]
  (let [entity (keyword entity)
        m2m-data (get-in module [:entities entity :fields (keyword m2m)])
        m2m-table (or (:table m2m-data) (keyword m2m))
        entity-table (or (get-in module [:entities entity :table])
                         entity)
        connecting-table (or (:connecting-table m2m-data)
                             (keyword (str (name entity) "_" (name m2m-table))))
        options (:options m2m-data)]
    {:options (map
               (fn [a]
                 [(get a (second options)) (get a (first options))])
               (map
                #(select-keys % options)
                  (k/select m2m-table
                            (k/join connecting-table (= (keyword (str
                                                                  (name connecting-table)
                                                                  "."
                                                                  (name m2m-table)
                                                                  "_id"))
                                                        :id))
                            (k/where {(keyword (str
                                                (name connecting-table)
                                                "."
                                                (name entity-table)
                                                "_id")) entity-id}))))
     :selected (map
                :id
                (k/select m2m-table
                          (k/fields :id)
                          (k/join connecting-table (= (keyword (str
                                                                (name connecting-table)
                                                                "."
                                                                (name m2m-table)
                                                                "_id"))
                                                      :id))
                          (k/where {(keyword (str
                                              (name connecting-table)
                                              "."
                                              (name entity-table)
                                              "_id")) entity-id})))}))

