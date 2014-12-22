(ns reverie.role)

(defprotocol RoleProtocol
  (allowed? [what user]))
