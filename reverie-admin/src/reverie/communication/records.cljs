(ns reverie.communication.records
  "Corresponding records from backend. Do not wish to use cljc for this yet.")

(defrecord User [id username email active?
                 created last-login
                 spoken-name full-name
                 roles groups])

