# Auth

Module shipped as default by reverie. Handles auth, users, roles and groups. Still not fully done, with especially authorization somewhat lacking.


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

```clojure
(ns some-namespace
  (:require [reverie.auth :as auth]))
  
  
(let [user (fetch-user 1)]

  ;; true if user has the role
  (auth/role? user :admin)
  
  ;; true if the user has an exact match of the roles
  (auth/role? user {:admin true :user false :staff false})

  ;; true if user has any role
  ;; vectors and lazy sequences also falls under this
  (auth/role? user #{:admin :user :staff}))
```


## User

Under the namespace reverie.auth a record exists called User. This is what you get back when you call reverie.auth/get-user or reverie.auth/get-users.


```clojure

;; Record User as defined in the code

(defrecord User [id username email active?
                 created last-login
                 spoken-name full-name
                 roles groups])
```

## functions / protocols

```clojure

(defprotocol IUserDatabase
  (get-users [db] "Get all users")
  (get-user [db] [db id-or-email] "Get user by session or by id/email"))

(defprotocol IUserLogin
  (login [data db]))
  
(defprotocol IUserToken
  (enable-token [id db] [id hours db])
  (retire-token [id db])
  (expired-token? [id db]))


(defprotocol IUserAdd
  (add-user! [data roles groups db]))

(defprotocol IUserUpdate
  (update! [user {:keys [username email spoken-name full-name active?] :as data) db])
  (set-password! [user new-password db]))

(defn logged-in? []
  (not (nil? (session/get :user-id))))

(defn logout []
  (session/clear!)
  true)


```

### logging in

```clojure
(ns some-namespace
  (:require [reverie.auth :as auth]
            some-namespace.user))
  
  
(defn some-response [{{db :database} :reverie :as request} page properties {:keys [username password] :as params}]
  ;; option 1
  (auth/login {:username username :password password} db)
  
  ;; option 2
  (let [user (auth/get-user db 1)] ;; get the first user
    ;; and then login with the user record
    (auth/login user db))
    
  ;; option 3
  (let [custom-record-user (some-namespace.user/get-custom-record-user username password)]
    ;; implement the IUserLogin protocol for the custom record and you can do this
    (auth/login custom-record-user db))
  )
```


### Resetting password with tokens

The protocol IUserToken gives support for password resetting functionality with tokens. reverie.auth.User, java.lang.Long, java.lang.Integer, java.util.UUID and java.lang.String (in the form of a UUID string) are all supported.


```clojure
(require '[reverie.auth :as auth])

;; by user id 1
(auth/enable-token 1 db)
(auth/expired-token? 1 db)
(auth/retire-token 1 db)
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
