(ns reverie.subs.auth
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :auth/logged-in?
 (fn [db _]
   (some? (:auth/profile db))))


(rf/reg-sub
 :auth/profile
 (fn [db _]
   (:auth/profile db)))
