(ns reverie.admin.storage
  (:refer-clojure :exclude [get assoc! dissoc!]))


(defprotocol IAdminStorage
  (assoc! [database k v])
  (dissoc! [database k])
  (get [database k]))
