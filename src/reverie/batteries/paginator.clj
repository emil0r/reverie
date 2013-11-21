(ns reverie.batteries.paginator)

(defmulti paginate
  "Paginate the incoming collection/length"
  (fn [coll? _ _] (sequential? coll?)))
(defmethod paginate true [coll count-per-page page]
  (paginate (count coll) count-per-page page))
(defmethod paginate :default [length count-per-page page]
  (let [pages (+ (int (/ length count-per-page))
                 (if (zero? (mod length count-per-page))
                   0
                   1))
        page (if (and (string? page)
                      (not= page ""))
               (read-string page)
               page)
        page (cond
              (nil? page) 1
              (or (neg? page) (zero? page)) 1
              (> page pages) pages
              :else page)
        next (+ page 1)
        prev (- page 1)]
    (reduce (fn [out k]
              (if (nil? k)
                out
                (cond
                 ;;
                 (and (= k :next-seq)
                      (not (nil? (:next out))))
                 (assoc out k (range (+ 1 (:next out))
                                     (+ 1 (:pages out))))
                 ;;
                 (and (= k :prev-seq)
                      (not (nil? (:prev out))))
                 (assoc out k (reverse (range 1
                                              (:prev out))))
                 ;;
                 :else out)))
            {:pages pages
             :page page
             :next (if (> next pages) nil next)
             :prev (if (or (neg? prev) (zero? prev)) nil prev)}
            [:next-seq :prev-seq])))
