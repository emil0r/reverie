(ns reverie.http.negotiation
  (:require [cognitect.transit :as transit]
            [reverie.auth :as auth]
            [muuntaja.core :as m]
            [tick.alpha.api :as t])
  (:import [reverie.auth User]))

(pr-str (auth/map->User {}))

(defn time-fn [o]
  (str o))

(def write-handlers {User (transit/write-handler "reverie.auth.User" pr-str)
                     java.time.Instant (transit/write-handler "time/instant" time-fn)
                     java.time.Month (transit/write-handler "time/month" time-fn)
                     java.time.DayOfWeek (transit/write-handler "time/day-of-week" time-fn)
                     java.time.Year (transit/write-handler "time/year" time-fn)})

(def read-handlers {"time/instant" (transit/read-handler (fn [x] (t/instant x)))
                    "time/month" (transit/read-handler (fn [x] (t/month x)))
                    "time/day-of-week" (transit/read-handler (fn [x] (t/day-of-week x)))
                    "time/year" (transit/read-handler (fn [x] (t/year x)))
                    "reverie.auth.User" (transit/read-handler auth/map->User)})

(def muuntaja-instance
  (m/create
   (update-in
    m/default-options
    [:formats "application/transit+json"]
    merge
    {:encoder-opts {:handlers write-handlers}
     :decoder-opts {:handlers read-handlers}})))
