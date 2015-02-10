(ns reverie.auth)


(defrecord User [id username email
                 created last-login
                 first-name last-name
                 roles groups])


(defprotocol UserDatabaseProtocol
  (get-users [db])
  (get-user [db id-or-email])
  (login [db username password]))
