(ns reverie.page.websocket
  (:require [org.httpkit.server :as server]
            [reverie.auth :as auth]
            [reverie.system :as sys]
            [reverie.websocket :refer [channels]]
            [taoensso.timbre :as log]))


(defn get-user-id [request]
  (or (auth/get-id request)
      (java.util.UUID/randomUUID)))

(defn render-fn [{:keys [options] :as this} request]
  (server/with-channel request channel
    (log/info (get-in this [:route :path]) "connected")
    (when-let [close (:on-close options)]
      (server/on-close channel close))
    (when-let [receive (:on-receive options)]
      (server/on-receive channel receive))
    (let [path (get-in this [:route :path])]
       (swap! sys/storage assoc-in [:websockets path :channel] channel))))
