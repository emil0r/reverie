(ns reverie.i18n
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [reverie.settings :as settings]
            [reverie.system :as sys]
            [reverie.util :refer [deep-merge]]
            [slingshot.slingshot :refer [throw+]]
            [taoensso.tower :as tower]
            [taoensso.timbre :as log]))

(def ^:dynamic *locale* nil)
(def t identity)
(def ^:private i18n-dictionary (atom {}))

(defmacro with-locale [locale & body]
  `(binding [*locale* (tower/jvm-locale ~locale)]
     ~@body))

(defn redef-t
  ([]
     (redef-t true))
  ([dev?]
     (alter-var-root
      #'t
      (fn [_]
        (let [tower-t (tower/make-t (assoc @i18n-dictionary :dev-mode? dev?))]
          (fn
            ([k-or-ks & fmt-args]
               (apply tower-t *locale* k-or-ks fmt-args))))))))

(defmulti get-i18n-dictionary class)
(defmethod get-i18n-dictionary java.lang.String [path]
  (let [config (or (io/resource path)
                   (->> (str/split path #"/")
                        (remove str/blank?)
                        (drop 1)
                        (apply str)
                        (io/resource)))]
    (if config
      (edn/read-string (slurp config)))))
(defmethod get-i18n-dictionary clojure.lang.IPersistentMap [data]
  data)
(defmethod get-i18n-dictionary nil [_]
  nil)
(defmethod get-i18n-dictionary :default [path]
  (throw+ {:what ::get-i18n-dictionary
           :path path}))

(defn add-i18n! [path-or-data]
  (->> path-or-data
       get-i18n-dictionary
       (deep-merge @i18n-dictionary)
       (reset! i18n-dictionary))
  (redef-t))

(defn load-from-options! [options]
  (if-let [dictionary (->> options
                           :i18n
                           get-i18n-dictionary)]
    ;; ideally this should only be loaded when we're in dev mode
    ;; however... during compilation this causes a fault in the compilation
    ;; so we'll leave it for now
    (add-i18n! dictionary)))

(defn get-i18n-path [[_ x]]
  (get-in x [:options :i18n]))

(defprotocol Ii18n
  (load-i18n! [component]))

(defrecord I18N [config prod? started?]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do
        (log/info "Starting i18n")
        (reset! i18n-dictionary config)
        (redef-t (not prod?))
        ;; for development when you're running things in the REPL
        (alter-var-root #'*locale* (fn [_] (:fallback-locale config)))
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
    (let [dictionary (->> [(map get-i18n-path (sys/modules))
                           (map get-i18n-path (sys/objects))
                           (map get-i18n-path (sys/apps))
                           (map get-i18n-path (sys/raw-pages))]
                          flatten
                          (remove nil?)
                          (map get-i18n-dictionary)
                          (apply deep-merge @i18n-dictionary))]
      (reset! i18n-dictionary dictionary)
      (redef-t (not prod?)))))

(defn get-i18n
  ([prod?]
     (map->I18N {:prod? prod? :config {:dictionary {}
                                       :dev-mode? true
                                       :fallback-locale :en}}))
  ([prod? config]
     (map->I18N {:config config :prod? prod?})))
