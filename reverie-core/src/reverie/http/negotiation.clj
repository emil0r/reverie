(ns reverie.http.negotiation
  (:require [cognitect.transit :as transit]
            [reverie.auth :as auth]
            [reverie.http.route :as route]
            [reverie.object :as object]
            [reverie.page :as page]
            [muuntaja.core :as m]
            [tick.alpha.api :as t])
  (:import [reverie.auth User]
           [reverie.http.route Route]
           [reverie.object ReverieObject]
           [reverie.page Page]))

(defn time-fn [o]
  (str o))

(defn- ->page [data]
  (pr-str (select-keys data [:id :serial :name :title :properties :template :created :updated :parent :version :slug :published-data :published?])))
(defn- <-page [data]
  (page/map->Page data))

(defn- ->route [data]
  (:path data))

(defn- <-route [data]
  (route/map->Route {:path data}))

(def write-handlers {User (transit/record-write-handler User)
                     Page (transit/write-handler "reverie.page.Page" ->page)
                     Route (transit/write-handler "reverie.http.route.Route" ->route)
                     ReverieObject (transit/record-write-handler ReverieObject) 
                     java.time.Instant (transit/write-handler "time/instant" time-fn)
                     java.time.Month (transit/write-handler "time/month" time-fn)
                     java.time.DayOfWeek (transit/write-handler "time/day-of-week" time-fn)
                     java.time.Year (transit/write-handler "time/year" time-fn)})

(def read-handlers {"reverie.auth.User" (transit/record-read-handler User)
                    "reverie.page.Page" (transit/read-handler <-page)
                    "reverie.http.route.Route" (transit/read-handler <-route)
                    "reverie.object.ReverieObject" (transit/record-read-handler ReverieObject)
                    "time/instant" (transit/read-handler (fn [x] (t/instant x)))
                    "time/month" (transit/read-handler (fn [x] (t/month x)))
                    "time/day-of-week" (transit/read-handler (fn [x] (t/day-of-week x)))
                    "time/year" (transit/read-handler (fn [x] (t/year x)))})

(def muuntaja-instance
  (m/create
   (update-in
    m/default-options
    [:formats "application/transit+json"]
    merge
    {:encoder-opts {:handlers write-handlers}
     :decoder-opts {:handlers read-handlers}})))
