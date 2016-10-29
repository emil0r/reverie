# page

Pages follow the the IPage protocol found in reverie.page.

```clojure

(defprotocol IPage
  (id [page])
  (serial [page])
  (parent [page])
  (root? [page])
  (children [page database])
  (children? [page database])
  (title [page])
  (name [page])
  (order [page])
  (options [page])
  (properties [page])
  (path [page])
  (slug [page])
  (objects [page])
  (type [page])
  (version [page])
  (published? [page])
  (created [page])
  (updated [page])
  (raw [page])
  (cache? [page]))

```
