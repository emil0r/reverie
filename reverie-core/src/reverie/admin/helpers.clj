(ns reverie.admin.helpers
  (:require [reverie.system :as sys]
            [reverie.util :as util]))



(defn get-template-names []
  (->> @sys/storage
       :templates
       (map (fn [[k _]]
              (util/kw->str k)))
       (remove (fn [k]
                 (.startsWith k "admin/")))
       sort))



(defn get-app-names []
  (->> @sys/storage
       :apps
       (map (fn [[k _]]
              (util/kw->str k)))
       sort))
