(ns reverie.event.auth
  (:require [akiroz.re-frame.storage :refer [persist-db]]
            [re-frame.core :as rf]
            [taoensso.timbre :as log]))

(rf/reg-event-fx
 :auth/profile
 [(persist-db :reverie :auth/profile)]
 (fn [{:keys [db]} [_ data]]   
   {:db (assoc db :auth/profile data)}))

(rf/reg-event-fx
 :auth/logout
 (fn [_ _]
   (log/info "Logging out")
   {:dispatch [:auth/profile nil]}))

