(ns reverie.publish)

(defprotocol PublishingProtocol
  (publish! [component])
  (published? [component])
  (unpublish! [component])
  (timed-publish! [component when])
  (trash! [component])
  (revisions [component])
  (revision [component revision])
  (history [component])
  (updates [component revision]))
