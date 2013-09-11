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

(defn normalize [str]
  (-> str
      (s/replace #"[åÅäÄĀāĀāÀÁÂÃÆàáâãæ]" "a")
      (s/replace #"[ČčÇç]" "c")
      (s/replace #"[Ðð]" "d")
      (s/replace #"[ĒēĒēËëÈÉÊËèéêë]" "e")
      (s/replace #"[Ğğ]" "g")
      (s/replace #"[ĪīĪīÏïİıìíîïÌÍÎÏ]" "i")
      (s/replace #"[Ĳĳ]" "ij")
      (s/replace #"[Ññ]" "n")
      (s/replace #"[öÖŐőŌōŌōŒœŒœòóôõöøÒÓÔÕÖØ]" "o")
      (s/replace #"[Þþ]" "p")
      (s/replace #"[Řř]" "r")
      (s/replace #"[ŠšŠšŠŞşŠš]" "s")
      (s/replace #"[ß]" "ss")
      (s/replace #"[ŰűŪūŪūÜüÙÚÛÜùúûü]" "u")
      (s/replace #"[ẀẁẂẃŴŵ]" "w")
      (s/replace #"[ŶŷŸýÝÿŸ]" "y")
      (s/replace #"[ŽžŽžŽžžŽ]" "z")
      (s/replace #"\s" "-")
      (s/replace #"\&" "-")
      (s/replace #"[^a-zA-Z0-9\-\_\.]" "")
      s/lower-case))

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

(defn join-uri
  "Join two or more fragmets of an URI together"
  [& uris]
  (loop [parts []
         [u & uris] uris]
    (if (nil? u)
      (str "/" (s/join "/" (flatten parts)))
      (recur (conj parts (remove s/blank? (s/split u #"/"))) uris))))

(defn uri-last
  "Take any uri and only return the last part corresponding to the page"
  [uri]
  (last (remove s/blank? (s/split uri #"/"))))

(defn uri-but-last
  "Take any uri and return everything but the last part corresponding to the page"
  [uri]
  (s/join "/" (butlast (remove s/blank? (s/split uri #"/")))))

