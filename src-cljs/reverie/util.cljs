(ns reverie.util)

(defn- strip-year [y]
  (apply str (drop 2 (str y))))

(defn- normalize [x]
  (if (< x 10)
    (str "0" x)
    x))

(defn date-format [s & [type]]
  (let [d (js/Date. s)]
    (case type
      :yymmdd (str (-> d .getFullYear strip-year)
                   "-"
                   (-> d .getMonth normalize)
                   "-"
                   (-> d .getDate normalize))
      :yyyymmdd (str (.getFullYear d)
                     "-"
                     (-> d .getMonth normalize)
                     "-"
                     (-> d .getDate normalize))
      (str (.getFullYear d)
           "-"
           (-> d .getMonth normalize)
           "-"
           (-> d .getDate normalize)
           " "
           (-> d .getHours normalize)
           ":"
           (-> d .getMinutes normalize)))))
