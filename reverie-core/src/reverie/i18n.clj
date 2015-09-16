(ns reverie.i18n
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [reverie.system :as sys]
            [reverie.util :refer [deep-merge]]
            [slingshot.slingshot :refer [throw+]]
            [taoensso.tower :as tower]
            [taoensso.timbre :as log]))


(def t identity)
(def ^:private i18n-dictionary (atom {}))

(defn redef-t
  ([]
     (redef-t true))
  ([dev?]
     (alter-var-root #'t (fn [_] (tower/make-t (assoc @i18n-dictionary
                                                 :dev-mode? dev?))))))

(defmulti get-i18n-dictionary class)
(defmethod get-i18n-dictionary java.lang.String [path]
  (edn/read-string
   (or (io/resource path)
       (->> (str/split path #"/")
            (remove str/blank?)
            (drop 1)
            (apply str)
            (io/resource)))))
(defmethod get-i18n-dictionary clojure.lang.IPersistentMap [data]
  data)
(defmethod get-i18n-dictionary :default [path]
  (throw+ {:what ::get-i18n-dictionary
           :path path}))

(defn add-i18n! [path-or-data]
  (->> path-or-data
       get-i18n-dictionary
       (deep-merge @i18n-dictionary)
       (reset! i18n-dictionary))
  (redef-t))

(defprotocol Ii18n
  (load-i18n! [component]))

(defrecord I18N [config dev? started?]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do
        (log/info "Starting i18n")
        (reset! i18n-dictionary config)
        (redef-t dev?)
        (assoc this :started? true))))
  (stop [this]
    (if-not started?
      this
      (do
        (log/info "Stopping i18n")
        (reset! i18n-dictionary {})
        (alter-var-root #'t (fn [_] identity))
        (assoc this
          :config nil
          :started? false))))
  Ii18n
  (load-i18n! [this]
    (let [dictionary (->> [(map :i18n (sys/modules))
                           (map :i18n (sys/objects))
                           (map :i18n (sys/apps))
                           (map :i18n (sys/raw-pages))]
                          flatten
                          (map get-i18n-dictionary)
                          (apply deep-merge @i18n-dictionary))]
      (reset! i18n-dictionary dictionary))))

(defn get-i18n [dev? config]
  (map->I18N {:config config :dev? dev?}))
