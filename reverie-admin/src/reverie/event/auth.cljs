(ns reverie.event.auth
  (:require [akiroz.re-frame.storage :refer [persist-db]]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]))

(rf/reg-event-fx
 :user/profile
 [(persist-db :reverie :user/profile)]
 (fn [{:keys [db]} [_ data]]   
   {:db (assoc db :user/profile data)}))

(rf/reg-event-fx
 :user/login
 (fn [_ [_ {:keys [username password]}]]
   {:dispatch [:http/post "/admin/auth" {:username username
                                         :password password}
               :user/profile]}))

(rf/reg-event-fx
 :user/logout
 (fn [_ _]
   (log/info "Logging out")
   {:dispatch [:user/profile nil]}))

