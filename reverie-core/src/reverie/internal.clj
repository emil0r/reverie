(ns reverie.internal
  "Internal storage")


(defonce storage (atom nil))

(defprotocol IInternalStorage
  (read-storage [storage key] "Read key")
  (write-storage [storage key value] "Write value to key")
  (delete-storage [storage key] "Delete key"))
