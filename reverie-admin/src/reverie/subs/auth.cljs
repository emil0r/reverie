(ns reverie.subs.auth
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :user/logged-in?
 (fn [db _]
   (some? (:user/profile db))))


(rf/reg-sub
 :user/profile
 (fn [db _]
   (:user/profile db)))
