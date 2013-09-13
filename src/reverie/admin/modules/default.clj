(ns reverie.admin.modules.default
  (:require [clojure.string :as s]
            [korma.core :as k])
  (:use [reverie.atoms :only [pages modules]]
        reverie.batteries.breadcrumbs
        reverie.batteries.paginator
        [reverie.core :only [defmodule]]
        [reverie.admin.templates :only [frame]]
        [reverie.admin.frames.common :only [frame-options]]))

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
        (sort (keys e)))))
(defn get-field-name [module entity field]
  (or (get-in module [:entities (keyword entity) :fields (keyword field) :name])
      (s/capitalize (name (get-in module [:entities (keyword entity) :fields (keyword field)])))))
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

(defn get-entity-name [module entity id]
  (if-let [display (first (get-display-fields module entity))]
    (get (first (k/select entity (k/where {:id id}))) display)
    id))


(defn navbar [{:keys [uri] :as request}]
  (let [{:keys [module module-name]} (get-module request)
        parts (remove s/blank? (s/split uri #"/"))
        uri-data (map
                  (fn [uri]
                    (cond
                     (re-find #"^\d+$" uri) [uri (get-entity-name
                                                  module
                                                  (first parts)
                                                  (read-string uri))]
                     :else [uri (s/capitalize uri)]))
                  parts)
        {:keys [crumbs]} (crumb uri-data {:base-uri (str "/admin/frame/module/" (name module-name))})]
    [:nav crumbs]))

(defmodule reverie-default-module {}
  [:get ["/"]
   (let [{:keys [module-name module]} (get-module request)]
     (frame
      frame-options
      [:div.holder
       [:nav]
       [:table.table.entities
        (map (fn [e]
               [:tr [:td [:a {:href (str "/admin/frame/module/"
                                         (name module-name)
                                         "/" (name e))}
                          (get-entity-name module e)]]])
             (keys (:entities module)))]]))]
  [:get ["/:entity"]
   (let [{:keys [module-name module]} (get-module request)
         display-fields (get-display-fields module entity)
         fields (get-fields module entity display-fields)
         {:keys [page]} (:params request)
         entities (get-entities module entity page)]
     (frame
      frame-options
      [:div.holder
       (navbar request)
       [:table.table.entity {:id entity}
        [:tr
         (map (fn [f]
                [:th (get-field-name module entity f)])
              display-fields)]
        (map #(get-entity-row % display-fields module-name entity) entities)]]))]

  [:get ["/:entity/:id" {:id #"\d+"}]
   (frame
    frame-options
    [:div.holder
     (navbar request)
     "my form"])]
  )
