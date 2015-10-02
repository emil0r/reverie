(ns reverie.settings
  (:require [com.stuartsierra.component :as component]
            [clojure.edn :as edn]
            [slingshot.slingshot :refer [throw+]]
            [taoensso.timbre :as log])
  (:refer-clojure :exclude [get true?]))

(defprotocol ISettings
  (true? [settings path expected])
  (prod? [settings])
  (dev? [settings])
  (initialized? [settings]))


(defrecord Settings [path settings initialized?]
  component/Lifecycle
  (start [this]
    (if initialized?
      this
      (do
        (log/info "Settings initialized")
        (assoc this
          :settings (-> path slurp edn/read-string)
          :initialized? true))))
  (stop [this]
    (if-not initialized?
      this
      (do
        (log/info "Settings cleared")
        (assoc this
          :settings nil
          :initialized? nil))))

  ISettings

  (true? [this path expected]
    (= expected (get-in settings path)))
  (initialized? [this] initialized?)
  (dev? [this] (= :dev (:server-mode settings)))
  (prod? [this] (= :prod (:server-mode settings))))

(defn get
  ([settings path]
     (get settings path nil))
  ([settings path default]
     (if-not (:initialized? settings)
       (throw+ {:type ::uninitialized-settings
                :message "Settings have not been initialized!"})

       (let [value (get-in (:settings settings) path default)]
         (if (and (nil? value)
                  (not= default nil))
           (throw+ :type ::path-not-found
                   :message (str "Nothing found for: " path))
           value)))))


(defn settings [path]
  (map->Settings {:path path}))
