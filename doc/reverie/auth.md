# Auth

Module shipped as default by reverie. Handles auth, users, roles and groups. Still not fully done with especially authorization somewhat lacking.


## authorization

```clojure

;; with-access required user to have required roles, otherwise exception is thrown

(with-access user #{:admin}
  ;; will only be returned if user has admin in their roles
  {:return-this (+ 1 1)})
```



## Groups

Groups has one function: grouping roles under one banner. Groups are then assigned to one or more users.

## Roles

[stub]


## User

Under the namespace reverie.auth a record exists called User. This is what you get back when you call reverie.auth/get-user or reverie.auth/get-users.


```clojure

;; Record User as defined in the code

(defrecord User [id username email active?
                 created last-login
                 spoken-name full-name
                 roles groups])
```


### Extending user

Sometimes you wish to extend the User record with your own data that is captured in addition to the data for the user record.

```clojure

(ns some-namespace
  (:require [ez-database.core :as db]
            [reverie.auth :as auth]))
           
;; in this example we wish to add an external table with data captured from OAuth logins
           
(auth/extend! :oauth (fn [{:keys [database user]}]
                       (db/query database {:select [:*]
                                           :from [:oauth_data]
                                           :where [:= :user_id (:id user)]})))
                                           

(let [db (reverie.system/get-db)
      user (auth/get-user db 1)]
  ;; data is lazily loaded and won't execute until dereffed
  @(:oauth user))
  
  
;; removing extensions are done by retract!
(auth/retract! :oauth)
```
