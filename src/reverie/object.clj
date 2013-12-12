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
                 (-> object (k/select (k/where {:id object-id})) first :page_id))
        order (-> object
                  (k/select
                   (k/aggregate (max :order) :order)
                   (k/where {:page_id page-id :area (util/kw->str area)}))
                  first
                  :order)]
    (cond
     (nil? order) 1
     (pos? order) (+ 1 order)
     (neg? order) 1
     :else 1)))

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

(defn get* [w]
  (k/select object (k/where w) (k/order :order)))

(defn get [object-id & [cmd]]
  (let [obj (-> object (k/select (k/where {:id object-id})) first)
        data (-> obj
                 :name
                 (get-object-entity)
                 (k/select (k/where {:object_id (:id obj)}))
                 first)]
    (case cmd
      :name-object [data (keyword (:name obj))]
      :data-object [data obj]
      data)))

(defn attributes? [data attributes]
  (= attributes (select-keys data (keys attributes))))

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

(defn copy! [object-id]
  (let [[obj {:keys [name area page_id]}] (get object-id :data-object)]
    (add! {:page-id page_id :name name :area area} (dissoc obj :object_id :id))))

(defn update! [object-id obj-data]
  (let [table (-> object (k/select (k/where {:id object-id})) first :name get-object-entity)]
    (k/update table
              (k/set-fields obj-data)
              (k/where {:object_id object-id}))))

(defn render [request]
  (let [[obj obj-name] (get (get-in request [:reverie :object-id]) :name-object)]
    (if-let [f (or
                (get-in @objects [obj-name (:request-method request)])
                (get-in @objects [obj-name :any]))]
      (f request obj (:params request)))))

(defmulti ^:private object-range
  "Range for objects depending on page type (apps have origo that objects can move around on an axis)"
  (fn [p hit-mode obj objs]
    [(:type p) hit-mode (cond
                         (= -1 (:order obj)) -1
                         (= 1 (:order obj)) 1
                         :else nil)]))
(defmethod ^:private object-range ["app" "up" 1] [p hit-mode obj objs]
  (let [-min (apply min (map :order objs))
        -max (apply max (map :order objs))]
    (map #(if (= % 1)
            -1
            (dec %)) (remove zero? (range -min (+ 1 -max))))))
(defmethod ^:private object-range ["app" "down" -1] [p hit-mode obj objs]
  (let [-min (apply min (map :order objs))
        -max (apply max (map :order objs))]
    (map #(if (= % -1)
            1
            (inc %)) (remove zero? (range -min (+ 1 -max))))))
(defmethod ^:private object-range ["app" "object-paste" nil] [p hit-mode obj objs]
  (let [-min (apply min (map :order objs))
        -max (apply max (map :order objs))]
    (remove zero? (range -min (+ 1 -max)))))
(defmethod ^:private object-range ["app" "object-paste" 1] [p hit-mode obj objs]
  (let [-min (apply min (map :order objs))
        -max (apply max (map :order objs))]
    (remove zero? (range -min (+ 1 -max)))))
(defmethod ^:private object-range ["app" "object-paste" -1] [p hit-mode obj objs]
  (let [-min (apply min (map :order objs))
        -max (apply max (map :order objs))]
    (remove zero? (range -min (+ 1 -max)))))
(defmethod ^:private object-range :default [p hit-mode obj objs]
  (let [-min (apply min (map :order objs))
        -max (apply max (map :order objs))]
    (remove zero? (range -min (+ 1 -max)))))

(defn move!
  "hit-mode is the action taken up the object
 anchor is the anchor to be moved to in area-paste hit-mode
 after-object-id is the object to moved after in the case of object-paste"
  [{:keys [object-id hit-mode anchor page-serial after-object-id]}]
  (let [obj (first (k/select object (k/where {:id object-id})))
        after-obj (if (nil? after-object-id)
                    nil
                    (-> (k/select object (k/where {:id after-object-id})) first))
        {app-path :app_path
         page-id :page_id
         area :area} (if (nil? after-object-id)
                             obj
                             after-obj)
        p (first (k/select page (k/where {:id page-id})))
        w (if (= (:type p) "app")
            {:page_id page-id :area area}
            {:page_id page-id :area area :app_path app-path})
        objs (k/select object
                       (k/where w))]
    (case hit-mode
      "object-paste" (let [{page-id :page_id
                            app-path :app_path} (first (k/select object
                                                                 (k/where {:id after-object-id})))
                            new-order (loop [loc (zip/vector-zip objs)]
                                        (let [next-loc (zip/next loc)]
                                          (if (zip/end? next-loc)
                                            (zip/root (zip/append-child (zip/vector-zip objs) obj))
                                            (let [node-value (zip/node next-loc)]
                                              (if (= (:id node-value) after-object-id)
                                                (zip/root (zip/insert-right next-loc obj))
                                                (recur next-loc))))))]

                       
                       (println (map (fn [{:keys [id name]}] [id name]) new-order))
                       (k/update object
                                 (k/set-fields {:area (util/kw->str anchor)
                                                :page_id page-id
                                                :app_path app-path})
                                 (k/where {:id object-id}))
                       (doseq [[{obj-id :id} order] (map vector new-order
                                                         (object-range p hit-mode obj new-order))]
                         (k/update object
                                   (k/set-fields {:order order})
                                   (k/where {:id obj-id})))
                       true)
      "area-paste" (let [{page-id :id} (first (k/select page
                                                        (k/where {:serial page-serial
                                                                  :version 0})))]
                     (k/update object
                               (k/set-fields {:order (get-last-order {:object-id object-id
                                                                      :area (util/kw->str anchor)})
                                              :area (util/kw->str anchor)
                                              :page_id page-id})
                               (k/where {:id object-id}))
                     true)
      "area" (do
               (k/update object
                         (k/set-fields {:order (get-last-order {:object-id object-id
                                                                :area (util/kw->str anchor)})
                                        :area (util/kw->str anchor)})
                         (k/where {:id object-id}))
               true)
      "up" (let [new-order (loop [loc (zip/vector-zip objs)]
                             (let [next-loc (zip/next loc)]
                               (if (zip/end? next-loc)
                                 (zip/root loc)
                                 (let [node-value (zip/node next-loc)]
                                   (if (= object-id (:id node-value))
                                     (if (or
                                          (beginning? next-loc)
                                          ;; check for this so that
                                          ;; the object moving up
                                          ;; doesn't jump any
                                          ;; potential object already
                                          ;; lying above origo
                                          (and
                                           (= "app" (:type p))
                                           (= 1 (:order node-value))))
                                       (zip/root next-loc)
                                       (-> next-loc
                                           zip/remove
                                           (zip/insert-left node-value)
                                           zip/root))
                                     (recur next-loc))))))]
             (doseq [[{obj-id :id} order] (map vector new-order
                                               (object-range p hit-mode obj new-order))]
               (k/update object
                         (k/set-fields {:order order})
                         (k/where {:id obj-id})))
             true)
      "down" (let [new-order (loop [loc (zip/vector-zip objs)]
                               (let [next-loc (zip/next loc)]
                                 (if (zip/end? next-loc)
                                   (zip/root loc)
                                   (let [node-value (zip/node next-loc)]
                                     (if (= object-id (:id node-value))
                                       (if (or
                                            (end? next-loc)
                                            ;; check for this so that
                                            ;; the object moving down
                                            ;; skips the origo instead
                                            ;; of the object next in
                                            ;; line if it's just below
                                            ;; the origo
                                            (and
                                             (= "app" (:type p))
                                             (= -1 (:order node-value))))
                                         (zip/root next-loc)
                                         (-> next-loc
                                             zip/remove
                                             zip/next
                                             (zip/insert-right node-value)
                                             zip/root))
                                       (recur next-loc))))))]
               (doseq [[{obj-id :id} order] (map vector new-order
                                                 (object-range p hit-mode obj new-order))]
                 (k/update object
                           (k/set-fields {:order order})
                           (k/where {:id obj-id})))
               true)
      "top" (let [new-order (loop [loc (zip/vector-zip objs)]
                              (let [next-loc (zip/next loc)]
                                (if (zip/end? next-loc)
                                  (zip/root loc)
                                  (let [node-value (zip/node next-loc)]
                                    (if (= object-id (:id node-value))
                                      (if (beginning? next-loc)
                                        (zip/root next-loc)
                                        (-> next-loc
                                            zip/remove
                                            zip/leftmost
                                            (zip/insert-left node-value)
                                            zip/root))
                                      (recur next-loc))))))]
              (doseq [[{obj-id :id} order] (map vector new-order
                                                (object-range p hit-mode obj new-order))]
                (k/update object
                          (k/set-fields {:order order})
                          (k/where {:id obj-id})))
              true)
      "bottom" (let [new-order (loop [loc (zip/vector-zip objs)]
                                 (let [next-loc (zip/next loc)]
                                   (if (zip/end? next-loc)
                                     (zip/root loc)
                                     (let [node-value (zip/node next-loc)]
                                       (if (= object-id (:id node-value))
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
                 (doseq [[{obj-id :id} order] (map vector new-order
                                                   (object-range p hit-mode obj new-order))]
                   (k/update object
                             (k/set-fields {:order order})
                             (k/where {:id obj-id})))
                 true)
      false)))


;; (doseq [[order id] [[-1 11] [1 10] [2 9]]]
;;   (k/update object (k/set-fields {:order order}) (k/where {:id id})))

;; (println (object-range {:type "app"}
;;                        "object-paste"
;;                        {:id 12 :order -1 :name :d}
;;                        [{:id 11 :order -1 :name :a}
;;                         {:id 9 :order 1 :name :b}
;;                         {:id 10 :order 2 :name :c}]))



(do
  (k/update object (k/set-fields {:page_id 1}) (k/where {:id 11}))
  (println
   "----\nbefore\n"
   (map (fn [{:keys [order id name]}] [order id name]) (k/select object (k/where {:page_id 2}))))
  
  (move! {:object-id 11 :hit-mode "object-paste" :after-object-id 9 :anchor "b"})
  
  (println
   "after\n"
   (map (fn [{:keys [order id name]}] [order id name]) (k/select object (k/where {:page_id 2})))))


