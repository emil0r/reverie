(ns reverie.admin.modules.helpers
  (:require [clojure.string :as s]
            [korma.core :as k])
  (:use [reverie.atoms :only [pages modules]]))

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
(defn get-field-name [module entity field]
  (or (get-in module [:entities (keyword entity) :fields (keyword field) :name])
      (s/capitalize (name field))))
(defn get-fields
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

