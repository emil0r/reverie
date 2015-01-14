(ns reverie.publish)

(defprotocol PublishingProtocol
  (publish! [database component])
  (unpublish! [database component])
  (timed-publish! [database component when])
  (trash! [database component])
  (revisions [database component])
  (revision [database component revision])
  (history [database component])
  (updates [database component revision]))
