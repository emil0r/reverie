(ns reverie.core
  (:require [reverie.admin.tree :as tree]))

(defn init []
  (tree/dev-init))
