(ns reverie.auth
  (:require [noir.session :as session]))


(defrecord User [id username email
                 created last-login
                 spoken-name full-name
                 roles groups])


(defprotocol IUserDatabase
  (get-users [db])
  (get-user [db] [db id-or-email]))

(defprotocol IUserLogin
  (login [data db]))


(defn logged-in? []
  (not (nil? (session/get :user-id))))

(defn logout []
  (session/clear!)
  true)
