(ns reverie.role
  (:require [clojure.set :as cset]
            [korma.core :as k]))


(defn add-roles [& roles]
  (let [roles (set (map name roles))
        existing-roles (set (map :name (k/select :role)))
        new-roles (cset/difference roles existing-roles)]
    (if (not (empty? new-roles))
     (k/insert :role
               (k/values (map
                          (fn [r] {:name r})
                          (cset/difference roles existing-roles)))))))
