(ns reverie.util
  (:require [clojure.string :as s]
            [jayq.core :as jq]))

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

(defn query-params
  ([]
     (query-params (-> js/window .-location .-href)
                   :keywordize-keys))
  ([params & [keywordize?]]
     (let [params (map
                   #(s/split % #"\=")
                   (-> params (s/split #"\?") last (s/split #"\&")))
           params (if (= keywordize? :keywordize-keys)
                    (map (fn [[k v]] [(keyword k) v]) params)
                    params)]
       (into {} params))))

(defn params->querystring [params]
  (s/join "&"
          (map (fn [[k v]] (str (name k) "=" v))
               (into [] params))))

(defn ev$ [e]
  (-> e .-target jq/$))


(defn activate! [elem$ & [sibling-path]]
  (jq/add-class elem$ :active)
  (let [file-name (jq/attr elem$ :name)
        siblings (if sibling-path
                   (remove #(= file-name (-> % .-attributes .-name .-value)) (jq/$ sibling-path))
                   (jq/siblings elem$))]
    (doseq [s siblings]
      (jq/remove-class (jq/$ s) :active))))
