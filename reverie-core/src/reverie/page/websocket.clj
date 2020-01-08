(ns reverie.page.websocket
  (:require [org.httpkit.server :as server]
            [reverie.system :as sys]
            [taoensso.timbre :as log]))


(defn render-fn [{:keys [options] :as this} request]
  (server/with-channel request channel
    (log/info (get-in this [:route :path]) "connected")
    (when-let [close (:on-close options)]
      (server/on-close channel close))
    (when-let [receive (:on-receive options)]
      (server/on-receive channel receive))
    (let [path (get-in this [:route :path])]
      (swap! sys/storage assoc-in [:websockets path :channel] channel))))
