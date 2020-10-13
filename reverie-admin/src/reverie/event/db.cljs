(ns reverie.event.db
  (:require [re-frame.core :as rf]))


(rf/reg-event-db :page/load (fn [db [_ {:keys [payload]}]]
                              (assoc db :reverie/pages payload)))
