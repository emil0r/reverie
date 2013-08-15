(ns reverie.test.auth.user
  (:require [reverie.auth.user :as user]
            [noir.session :as session]
            [noir.cookies :as cookies]
            [korma.core :as k])
  (:use midje.sweet
        reverie.entity
        reverie.test.helpers))


(k/delete user)
(k/delete role)

(fact
 "user/add"
 (user/add! {:first-name "Emil" :last-name "Bengtsson"
             :email "emil0r@gmail.com" :name "admin" :password "admin0r"
             :is-staff true :is-admin true}) => true)




(fact
 "user/login"
 (with-noir
   (user/login "admin" "admin0r")) => true)

(fact
 "user/login user-does-not-exist"
 (with-noir
   (user/login "false" "false")) => :user-does-not-exist)


(fact
 "user/logged-in?"
 (with-noir
   (user/login "admin" "admin0r")
   (user/logged-in?)) => true)
