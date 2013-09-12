(ns reverie.batteries.paginator)



(defmulti paginate (fn [coll? _ _] (sequential? coll?)))
(defmethod paginate true [coll count-per-page page]
  (paginate (count coll) count-per-page page))
(defmethod paginate :default [length count-per-page page]
  (let [pages (+ (int (/ length count-per-page))
                 (if (zero? (mod length count-per-page))
                   0
                   1))
        page (cond
              (or (neg? page) (zero? page)) 1
              (> page pages) pages
              :else page)
        next (+ page 1)
        prev (- page 1)]
   {:pages pages
    :page page
    :next (if (> next pages) nil next)
    :prev (if (or (neg? prev) (zero? prev)) nil prev)}))
