(ns reverie.publish)

(defprotocol PublishingProtocol
  (publish-page! [database page-id] [database page-id recur?])
  (unpublish-page! [database page-id])
  (trash-page! [database page-id])
  (trash-object! [database obj-id]))

;; (revisions [database page-id])
;; (revision [database page-id revision])
;; (history [database page-id])
;; (updates [database page-id revision])
