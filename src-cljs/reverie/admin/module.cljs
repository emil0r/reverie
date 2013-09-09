(ns reverie.admin.module
  "Run for the module frame as the default"
  (:require [jayq.core :as jq]
            [jayq.util :as util]))


(defn init []
  (util/log "module init..."))
