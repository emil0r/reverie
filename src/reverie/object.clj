(ns reverie.object
  (:refer-clojure :exclude [get meta])
  (:require [clojure.string :as s]
            [clout.core :as clout]
            [korma.core :as k]
            [reverie.util :as util])
  (:use reverie.atoms
        reverie.entity))


(defn get [request & [cmd]]
  (let [object-id (get-in request [:reverie :object-id])
        obj (-> object (k/select (k/where {:id object-id})) first)
        data (-> obj
                 :name
                 (get-object-entity)
                 (k/select (k/where {:object_id (:id obj)}))
                 first)]
    (case cmd
      :name-object [data (keyword (:name obj))]
      data)))

(defn add! [{:keys [page-id name area]} obj]
  (let [name (clojure.core/name name)
        page-obj (k/insert object
                           (k/values {:page_id page-id :updated (k/sqlfn now)
                                      :name name
                                      :area (util/kw->str area)}))
        
        real-obj (k/insert (get-object-entity name)
                           (k/values (assoc obj :object_id (:id page-obj))))]
    page-obj))

(defn render [request]
  (let [[obj obj-name] (get request :name-object)]
    (if-let [f (or
                (get-in @objects [obj-name (:request-method request)])
                (get-in @objects [obj-name :any]))]
      (if (util/mode? request :edit)
        [:div.reverie-object {:object-id (:object_id obj)}
         [:div.reverie-object-holder
          [:span.reverie-object-panel (str "object " (name obj-name))]]
         (f request obj)]
        (f request obj)))))
