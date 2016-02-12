# Object

Implementation details for objects are spread out over a number of files. Primarily you will want to look at src/reverie/object.clj. The defobject macro is defined in core.


```clojure

(defn myobject [request object properties params]
  ;; - request is the incomg request
  ;; - object is the initated reverie.object/ReverieObject record
  ;; - properties are the properties the object holds. this 
  ;;   corresponds directly with the database table
  ;; - params are the query/form params from the request
  )

(defobject myobject
  {;; table name for the object
   :table "objects_myobject
   
   ;; migration path and whether you want it to be automatically
   ;; applied when the system starts up
   ;; see migrations for more info on how to use migrations
   :migration {:path "src/myproject/objects/migrations/barcode"
               :automatic? true}
               
   ;; each field key corresponds directly to a 
   ;; column in the object table
   :fields {:mode {:name "Mode"
                   :type :dropdown
                   :options ["" 
                             "mode-awesome"
                             "mode-more-awesome"]
                   :initial ""}
            :header {:name "Header"
                     :type :text
                     :initial ""
                     :help "Help text that shows up"}
            :info {:name "Info"
                   :type :richtext
                   :inline? true
                   :initial ""}}
   :sections [{:fields [:mode :header :info]}]}
  {:any myobject})

```


## Migrations

See [concepts/migrations](../concepts/migrations.md).

## fields

See [reverie/fields](fields.md).


## sections

Sections take optionally a :name field. Each section is displayed in the admin interface as a fieldset with the :name field as a legend if available.
