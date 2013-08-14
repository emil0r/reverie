(ns reverie.object
  (:refer-clojure :exclude [get meta])
  (:require [clojure.string :as s]
            [clout.core :as clout]
            [korma.core :as k]
            [reverie.util :as util])
  (:use [reverie.core :exclude [objects]]
        reverie.entity))


(defn- get-serial-object []
  (let [serial (-> object (k/select (k/aggregate (max :serial) :serial)) first :serial)]
    (if serial
      (+ 1 serial)
      1)))


(defn render [{:keys [object-id] :as request}]
  )

(defn get [{:keys [object-id serial version] :as request}]
  (let [w (if object-id
            {:id object-id}
            {:serial serial :version version})
        obj (-> object (k/select (k/where w)) first)]
    (first (k/select
            (get-object-entity (:name obj))
            (k/where {:object_id (:id obj)})))))

(defn add! [{:keys [page-id] :as request} meta obj]
  (let [page-obj (k/insert object
                           (k/values {:page_id page-id :updated (k/sqlfn now)
                                      :name (:name meta)
                                      :area (-> meta :area util/kw->str)
                                      :version 0 :serial (get-serial-object)}))

        real-obj (k/insert (get-object-entity (:name meta))
                           (k/values (assoc obj :object_id (:id page-obj))))]
    page-obj))
