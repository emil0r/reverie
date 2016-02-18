# route

Routes in reverie are supported by the [clout](https://github.com/weavejester/clout) library.


## HTTP methods

All HTTP methods are supported, lower case only. :any key is used to match all HTTP method. If a key is given for a specific HTTP method and :any is also specified, the key specified will take precedence.

## Example

```clojure

;; vector of routes

[ ["/" ;; URI
   {:any my-any-fn :get my-get-fn}] ;; map of HTTP methods
  
  ["/:foo" ;; foo will passed along as a param in params
   {:foo #"\w+"} ;; limit foo by regex
   {:any my-any-fn}]
  
  ["/:foo/:bar"
   #{:bar #"^\d+$"
     :foo #"\w+"}
   {:bar Integer} ;; cast bar to an Integer. USE WITH CAUTION. See below on casting
   {:any my-any-fn}]
    
]
```


## Casting

Technically reverie supports casting, but the code was written before [Schema](https://github.com/plumatic/schema) was written and is planned to be replaced by Schema. Use with caution.


## URI

Any request map going through a route will have a :shortened-uri added to it. This is the URI after the routing takes place.
