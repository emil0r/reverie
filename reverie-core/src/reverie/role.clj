(ns reverie.role)

(defprotocol RoleProtocol
  (allowed? [what user] [what user operation]))
