(ns reverie.publish)

(defprotocol PublishingProtocol
  (publish! [database component] [database component when])
  (unpublish! [database component])
  (trash! [database component])
  (revisions [database component])
  (revision [database component revision])
  (history [database component])
  (updates [database component revision]))
