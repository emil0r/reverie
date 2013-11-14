(ns reverie.admin.modules.helpers
  (:require [clojure.string :as s]
            [korma.core :as k])
  (:use [reverie.atoms :only [modules]]
        reverie.batteries.breadcrumbs
        reverie.batteries.paginator
        [reverie.util :only [join-uri]]))

(defn get-module [{:keys [real-uri]}]
  (let [name (-> real-uri
                 (s/replace #"/admin/frame/module/" "")
                 (s/split #"/")
                 first
                 keyword)]
    {:module-name name
     :module (@modules name)}))


(defn get-entity-table [entity module]
  (or (get-in module [:entities entity :table])
      (keyword entity)))

(defn get-entity-default-data [entity module]
  (let [fields (get-in module [:entities (keyword entity) :fields])]
    (reduce (fn [out [field data]]
              (if (nil? field)
                out
                (if (:default data)
                  (assoc out field (:default data))
                  out)))
            {}
            fields)))

(defn field-type? [type]
  (fn [[_ v]]
    (= (:type v) type)))

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



(defn pre-process-data [data mode module entity]
  (if-let [pre (get-in module [:entities (keyword entity) :pre])]
    (pre data mode)
    data))

(defn post-process-data [data mode module entity]
  (if-let [post (get-in module [:entities (keyword entity) :post])]
    (post data mode)
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

(defn table-field
  ([table field]
     (table-field table field ""))
  ([table field extra]
     (keyword (str
               (name table)
               "."
               (name field)
               (name extra)))))

(defn drop-down-m2m-data [module entity m2m form-data entity-id]
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
                (k/select m2m-table)))
     :selected (or
                (get form-data m2m)
                (if entity-id
                  (let [entity-id (read-string entity-id)]
                    (map
                     :id
                     (k/select m2m-table
                               (k/fields :id)
                               (k/join connecting-table (= (table-field connecting-table
                                                                        m2m-table
                                                                        :_id)
                                                           :id))
                               (k/where {(table-field connecting-table
                                                      entity-table
                                                      :_id) entity-id}))))
                  
                  []))}))



(defn save-m2m-data [module entity entity-id form-data]
  (let [entity (keyword entity)
        entity-table (or (get-in module [:entities entity :table])
                         entity)]
    (doseq [[field field-data] (filter (field-type? :m2m)
                                       (get-in module [:entities entity :fields]))]
      (let [field-table (or (:table field-data) field)
            connecting-table (or (:connecting-table field-data)
                                 (keyword (str (name entity) "_" (name field-table))))
            ce_id (table-field connecting-table entity-table :_id)
            insert-data (map (fn [v] {(keyword (str (name entity-table) "_id"))
                                     entity-id
                                     (keyword (str (name field-table) "_id"))
                                     v}) (form-data field))]
        (k/delete connecting-table
                  (k/where {ce_id entity-id}))
        (if (not (empty? insert-data))
          (k/insert connecting-table
                    (k/values insert-data)))))))





;; html and stuff
(defn frame-join-uri [& uris]
  (apply join-uri "/admin/frame/module/" (map name uris)))

(defn navbar [{:keys [uri real-uri] :as request}]
  (let [uri (last (s/split real-uri #"^/admin/frame/module"))
        {:keys [module module-name]} (get-module request)
        parts (remove s/blank? (s/split uri #"/"))
        uri-data (map
                  (fn [uri]
                    (cond
                     (re-find #"^\d+$" uri) [uri (get-instance-name
                                                  module
                                                  (second parts)
                                                  (read-string uri))]
                     :else [uri (get-entity-name module uri)]))
                  parts)
        {:keys [crumbs]} (crumb uri-data {:base-uri "/admin/frame/module/"})]
    [:nav crumbs]))

(defn- page-url [base-uri current? page]
  (if page
    [:li.page
     (if current?
       [:span page]
       [:a {:href (str base-uri "?page=" (str page))} page])]))

(defn plural? [length singular plural]
  (if (> length 1)
    (str length " " plural)
    (str length " " singular)))

(defn pagination [length count-per-page page base-uri]
  (let [paginated (paginate length count-per-page page)]
    
    [:div.pagination
     [:div.num-pages
      (str (plural? (:pages paginated) "page" "pages")
           ", "
           (plural? length "item" "items"))]
     (if (> (:pages paginated) 1)
      [:ul
       (map (partial page-url base-uri false) (reverse (take 4 (:prev-seq paginated))))
       (page-url base-uri false (:prev paginated))
       (page-url base-uri true (if page page 1))
       (page-url base-uri false (:next paginated))
       (map (partial page-url base-uri false) (take 4 (:next-seq paginated)))])]))


(defn form-help-text [field-data]
  (if (:help field-data)
    [:div.help [:i.icon-question-sign] (:help field-data)]))
