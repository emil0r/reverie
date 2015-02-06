(ns reverie.movement
  (:require [clojure.zip :as zip]))


(defn- beginning? [loc]
  (= (zip/node loc) (zip/node (zip/leftmost loc))))
(defn- end? [loc]
  (= (zip/node loc) (zip/node (zip/rightmost loc))))

(defn insert-origo [ids]
  (let [ids (vec ids)]
    (if (empty? ids)
      [[0 nil]]
      (loop [loc1 (zip/next (zip/vector-zip ids))
             val1 (zip/node loc1)]
        (let [loc2 (if (-> loc1 zip/next zip/next zip/end?)
                     nil
                     (-> loc1 zip/right))
              val2 (if (nil? loc2)
                     nil
                     (zip/node loc2))]
          (cond
           (zero? (first val1)) (zip/root loc1)
           (and (neg? (first val1)) (nil? val2))
           (-> loc1
               (zip/insert-right [0 nil])
               zip/root)
           (and (pos? (first val1)) (nil? val2))
           (-> loc1
               (zip/insert-left [0 nil])
               zip/root)
           (and (neg? (first val1)) (pos? (first val2)))
           (-> loc1
               (zip/insert-right [0 nil])
               zip/root)
           (and (pos? (first val1)) (pos? (first val2)))
           (-> loc1
               (zip/insert-left [0 nil])
               zip/root)
           (zip/end? loc1) (zip/root loc1)
           :else (recur (zip/right loc1) (zip/node (zip/right loc1)))))))))

(defn- dir-up [ids id]
  (loop [loc (zip/vector-zip ids)]
    (let [next-loc (zip/next loc)]
      (if (zip/end? next-loc)
        (zip/root loc)
        (let [node-value (zip/node next-loc)]
          (if (= id node-value)
            (if (beginning? next-loc)
              (zip/root next-loc)
              (-> next-loc
                  zip/remove
                  (zip/insert-left node-value)
                  zip/root))
            (recur next-loc)))))))

(defn- dir-down [ids id]
  (loop [loc (zip/vector-zip ids)]
    (let [next-loc (zip/next loc)]
      (if (zip/end? next-loc)
        (zip/root loc)
        (let [node-value (zip/node next-loc)]
          (if (= id node-value)
            (if (end? next-loc)
              (zip/root next-loc)
              (-> next-loc
                  zip/remove
                  zip/next
                  (zip/insert-right node-value)
                  zip/root))
            (recur next-loc)))))))

(defn- dir-top [ids id]
  (loop [loc (zip/vector-zip ids)]
    (let [next-loc (zip/next loc)]
      (if (zip/end? next-loc)
        (zip/root loc)
        (let [node-value (zip/node next-loc)]
          (if (= id node-value)
            (if (beginning? next-loc)
              (zip/root next-loc)
              (-> next-loc
                  zip/remove
                  zip/leftmost
                  (zip/insert-left node-value)
                  zip/root))
            (recur next-loc)))))))

(defn- dir-bottom [ids id]
  (loop [loc (zip/vector-zip ids)]
    (let [next-loc (zip/next loc)]
      (if (zip/end? next-loc)
        (zip/root loc)
        (let [node-value (zip/node next-loc)]
          (if (= id node-value)
            (do
              (if (end? next-loc)
                (zip/root next-loc)
                (-> next-loc
                    zip/remove
                    zip/next
                    zip/rightmost
                    (zip/insert-right node-value)
                    zip/root)))
            (recur next-loc)))))))

(defn- get-zero-index [ids]
  (->> ids
       (map-indexed (fn [index id] [index id]))
       (filter (fn [[index id]] (nil? id)))
       ffirst))

(defn- re-index [ids origo?]
  (if origo?
    (let [z (get-zero-index ids)]
      (vec (map-indexed (fn [index id]
                          [(- index z) id]) ids)))
    (vec (map-indexed (fn [index id]
                        [(inc index) id]) ids))))

(defn move "ids -> [[order id] [order id] ...]"
  ([ids id direction]
     (move ids id direction false))
  ([ids id direction origo?]
     (let [ids (vec (map second (if origo?
                                      (insert-origo ids)
                                      ids)))
           ids-ordered
           (case (keyword direction)
             :up (dir-up ids id)
             :down (dir-down ids id)
             :top (dir-top ids id)
             :bottom (dir-bottom ids id))]
       (re-index ids-ordered origo?))))

(defn after "ids -> [[order id] [order id] ...]"
  ([ids id new-id]
     (after ids id new-id false))
  ([ids id new-id origo?]
     (let [ids (vec (map second (if origo?
                                  (insert-origo ids)
                                  ids)))]
       (loop [loc (zip/vector-zip ids)]
         (let [next-loc (zip/next loc)]
           (if (zip/end? next-loc)
             (re-index
              (zip/root (zip/append-child (zip/vector-zip ids) new-id))
              origo?)
             (let [node-value (zip/node next-loc)]
               (if (= node-value id)
                 (re-index
                  (zip/root (zip/insert-right next-loc new-id))
                  origo?)
                 (recur next-loc)))))))))

(defn before "ids -> [[order id] [order id] ...]"
  ([ids id new-id]
     (before ids id new-id false))
  ([ids id new-id origo?]
     (let [ids (vec (map second (if origo?
                                  (insert-origo ids)
                                  ids)))]
       (loop [loc (zip/vector-zip ids)]
         (let [next-loc (zip/next loc)]
           (if (zip/end? next-loc)
             (re-index
              (zip/root (zip/append-child (zip/vector-zip ids) new-id))
              origo?)
             (let [node-value (zip/node next-loc)]
               (if (= node-value id)
                 (re-index
                  (zip/root (zip/insert-left next-loc new-id))
                  origo?)
                 (recur next-loc)))))))))
