(ns reverie.event.init
  (:require [reverie.db :as db]
            [re-frame.core :as rf]))

(rf/reg-event-fx
 ::initialize
 (fn [_ _]
   db/default-db))
