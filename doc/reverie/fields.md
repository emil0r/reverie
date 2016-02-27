# fields 

Both [reverie.object](object.md) and [reverie.module](module.md) take fields as arguments. A field always corresponds to a column in a database.


## Implementation

All fields are implemented through the multimethod row located in the reverie.admin.looknfeel.form namespace. Implementing new fields are possible by extending the multimethod. reverie's filemanager does this.


## The fields

All fields have the following arguments in common:

- :name "This is what will show up in the admin interface"
- :initial "This is what reverie will initialize a new entry with in the database. Take care to avoid nil values if you have set NOT NULL in the database."
- :help "This text will show up as a help text in the admin interface."

### :html
Custom HTML representation of a field.

```clojure
   {;; other stuff
    :type :html
    :html (fn [entity field data] "do something with this")}
```

### :m2m

Many to many. Bind to another table through a linking table.

```clojure
   {;; other stuff
    :type :m2m
    
    ;; optional casting, but is normally done as m2m is usually
    ;; held together with foreign keys in the database
    :cast :int

    ;; table we want to have joined
    :table :auth_group
    
    ;; select options for hiccup
    :options [:id :name]

    ;; order by
    :order :name
    
    ;; the joining
    :m2m {;; the joining table
          :table :auth_user_group
          
          ;; joining: this that
          ;; this: this table's foreign key
          ;; that: the joining table's foreign key
          :joining [:user_id :group_id]}}

```


### :richtext

Richtext field. Uses tinyMCE.

```clojure
   {;; other stuff
    :type :richtext
    :inline? true}
```


### :boolean

True/false checkbox.


```clojure
   {;; other stuff
    :type :boolean}
```


### :dropdown

:options expects a hiccup vector. It takes optionally a function expecting a hiccup vector back.

```clojure
;; optionally a function can be used as :options

(defn get-my-options [{:keys [database]}]
  ;; do something with the database, or fetch the data from
  ;; somewhere else
  
  (let [query "SELECT id, name FROM test_table"
        result (ez-database.core/query database query)
        options-returned-as-hiccup-vector (map (juxt :id :name) result)]
        options-returned-as-hiccup-vector))

   {;; other stuff
    :type :dropdown
    :options ["option1"
              "option2"]}
```


### :email

Input type in the HTML is set to type="email".

```clojure
   {;; other stuff
    :type :email}
```


### :number

Input type in the HTML is set to type="number". The input is cast to a number in Clojure.

```clojure
   {;; other stuff
    :type :number}
```


### :slug

A slug field. Used for sanitizing another field which you wish to use as part of a URL.

```clojure
   {;; other stuff
    :type :slug
    :for :name-of-field-you-wish-to-slugify}
```

### :textarea

```clojure
   {;; other stuff
    :type :textarea}
```


### :datetime

Allows for datetime picking with javascript. Converts to java.sql.Timestamp in the background.

```clojure
   {;; other stuff
    :type :datetime}
```

### :image

Pick an image using the filemanager.

```clojure
   {;; other stuff
    :type :image}
```


### anything else

There is a :default implementation for the multimethod in question. It spits out a text field with anything extra in there.
