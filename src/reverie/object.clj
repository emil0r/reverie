(ns reverie.object
  (:refer-clojure :exclude [get meta])
  (:require [clojure.string :as s]
            [clojure.zip :as zip]
            [clout.core :as clout]
            [korma.core :as k]
            [reverie.util :as util])
  (:use reverie.atoms
        reverie.entity))

(defn- get-last-order [{:keys [object-id area page-id]}]
  (let [page-id (or
                 page-id
                 (-> object (k/select (k/where {:id object-id})) first :page_id))]
    (+ 1
       (or
        (-> object (k/select (k/aggregate (max :order) :order)
                             (k/where {:page_id page-id :area (util/kw->str area)})) first :order)
        0))))

(defn- get-serial-object []
  (let [serial (-> object (k/select (k/aggregate (max :serial) :serial)) first :serial)]
    (if serial
      (+ 1 serial)
      1)))

(defn- beginning?
  "Used for move!"
  [loc]
  (= (zip/node loc) (zip/node (zip/leftmost loc))))
(defn- end?
  "Used for move!"
  [loc]
  (= (zip/node loc) (zip/node (zip/rightmost loc))))


(defn get-attributes [name]
  (let [name (keyword name)]
    (-> @objects name :options :attributes)))

(defn get-attributes-order
  "Get order in which the attributes are supposed to be in. Will look for attributes-order in options. If nothing is found it will send back a sorted key-list of the attributes."
  [name]
  (let [name (keyword name)]
    (or
     (-> @objects name :options :attributes-order)
     (sort (keys (get-attributes name))))))

(defn get [object-id & [cmd]]
  (let [obj (-> object (k/select (k/where {:id object-id})) first)
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
                                      :area (util/kw->str area)
                                      :serial (get-serial-object)
                                      :order (get-last-order {:page-id page-id
                                                              :area (util/kw->str area)})}))
        
        real-obj (k/insert (get-object-entity name)
                           (k/values (assoc obj :object_id (:id page-obj))))]
    page-obj))

(defn update! [object-id obj-data]
  (let [table-name (-> object (k/select (k/where {:id object-id})) first :name keyword)]
    (k/update table-name
              (k/set-fields obj-data)
              (k/where {:object_id object-id}))))

(defn render [request]
  (let [[obj obj-name] (get (get-in request [:reverie :object-id]) :name-object)]
    (if-let [f (or
                (get-in @objects [obj-name (:request-method request)])
                (get-in @objects [obj-name :any]))]
      (if (util/mode? request :edit)
        [:div.reverie-object {:object-id (:object_id obj)}
         [:div.reverie-object-holder
          [:span.reverie-object-panel (str "object " (name obj-name))]]
         (f request obj)]
        (f request obj)))))

(defn move! [{:keys [object-id hit-mode anchor]}]
  (let [{page-id :page_id area :area} (-> object (k/select (k/where {:id object-id})) first)
        objs (vec (map :id (k/select object
                                     (k/where {:page_id page-id
                                               :area area}))))]
    (case hit-mode
      "area" (do
               (k/update object
                         (k/set-fields {:order (get-last-order {:object-id object-id
                                                                :area (util/kw->str anchor)})
                                        :area (util/kw->str anchor)})
                         (k/where {:id object-id}))
               true)
      "up" (let [new-order (loop [loc (zip/vector-zip objs)]
                             (let [next-loc (zip/next loc)]
                               (if (nil? next-loc)
                                 (zip/root loc)
                                 (let [node-value (zip/node next-loc)]
                                   (if (= object-id node-value)
                                     (if (beginning? next-loc)
                                       (zip/root next-loc)
                                       (-> next-loc
                                           zip/remove
                                           (zip/insert-left node-value)
                                           zip/root))
                                     (recur next-loc))))))]
             (doseq [[obj-id order] (map vector new-order (range 1 (+ 1 (count new-order))))]
               (k/update object
                         (k/set-fields {:order order})
                         (k/where {:id obj-id})))
             true)
      "down" (let [new-order (loop [loc (zip/vector-zip objs)]
                               (let [next-loc (zip/next loc)]
                                 (if (nil? next-loc)
                                   (zip/root loc)
                                   (let [node-value (zip/node next-loc)]
                                     (if (= object-id node-value)
                                       (if (end? next-loc)
                                         (zip/root next-loc)
                                         (-> next-loc
                                             zip/remove
                                             zip/next
                                             (zip/insert-right node-value)
                                             zip/root))
                                       (recur next-loc))))))]
               (doseq [[obj-id order] (map vector new-order (range 1 (+ 1 (count new-order))))]
                 (k/update object
                           (k/set-fields {:order order})
                           (k/where {:id obj-id})))
               true)
      "top" (let [new-order (loop [loc (zip/vector-zip objs)]
                              (let [next-loc (zip/next loc)]
                                (if (nil? next-loc)
                                  (zip/root loc)
                                  (let [node-value (zip/node next-loc)]
                                    (if (= object-id node-value)
                                      (if (beginning? next-loc)
                                        (zip/root next-loc)
                                        (-> next-loc
                                            zip/remove
                                            zip/leftmost
                                            (zip/insert-left node-value)
                                            zip/root))
                                      (recur next-loc))))))]
              (doseq [[obj-id order] (map vector new-order (range 1 (+ 1 (count new-order))))]
                (k/update object
                          (k/set-fields {:order order})
                          (k/where {:id obj-id})))
              true)
      "bottom" (let [new-order (loop [loc (zip/vector-zip objs)]
                                 (let [next-loc (zip/next loc)]
                                   (if (nil? next-loc)
                                     (zip/root loc)
                                     (let [node-value (zip/node next-loc)]
                                       (if (= object-id node-value)
                                         (do
                                           (if (end? next-loc)
                                             (zip/root next-loc)
                                             (-> next-loc
                                                 zip/remove
                                                 zip/next
                                                 zip/rightmost
                                                 (zip/insert-right node-value)
                                                 zip/root)))
                                         (recur next-loc))))))]
                 (doseq [[obj-id order] (map vector new-order (range 1 (+ 1 (count new-order))))]
                   (k/update object
                             (k/set-fields {:order order})
                             (k/where {:id obj-id})))
                 true)
      false)))
