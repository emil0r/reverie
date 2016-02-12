# server
namespace: reverie.server

## options
    - dev?: if true wrap-reload and wrap-stacktrace is added to the middleware
    - run-server: function that starts the server
    - stop-server: function that stops the server
    - server-options: hashmap with all the options sent to the server
    - site-handlers [optional]: provide if you wish to define your own handler sequence
    - extra-handlers [optional]: optional extra handlers to add to the chain. they will be executed after the site-handler chain has been executed.
    - middleware-options: options for middleware defined in the default handler chains
   
### extra-handlers
Provided in the format of
```clojure
[[wrap-one]
 [wrap-two]
 [wrap-three]]
```

### middleware-options

```clojure
  ;; in default site handler chain
  [wrap-content-type (:content-type middleware-options)]
  [wrap-multipart-params
    (:multipart-opts middleware-options)]
  [wrap-noir-session
    {:store (or store (memory-store))
    ;; store session
    ;; cookies for 360 days
    :cookie-attrs {:max-age (get-in middleware-options [:cookie :max-age] 31104000)}}]
  ;; in default resource handler chain
  [wrap-file-info (:mime-types middleware-options)]
  ;; in default media handler chain
  [wrap-file-info (:mime-types middleware-options)]
  [wrap-content-type (:content-type-resources middleware-options)]
```
