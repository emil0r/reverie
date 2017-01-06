(ns reverie.email
  (:require
   [clojure.core.async :as async]
   [clojure.string :as str]
   [com.stuartsierra.component :as component]
   [ez-email.core :as email]
   [taoensso.timbre :as log]))



(def queue-message email/queue-message)


(defrecord EmailManager [started? provider provider-settings]
  component/Lifecycle
  (start [this]
    (if (true? started?)
      this
      (let [provider (or provider (email/get-provider provider-settings))]
        (log/info "Starting EmailManager")
        (if-let [c (:c-result provider)]
          (async/go-loop []
            (when-let [result (async/<! c)]
              (log/info {:what ::EmailManager
                         :result result})
              (recur)))
          (log/error {:what ::EmailManager
                      :message "Missing c-result channel from email provider"}))


        (assoc this
               :started? true
               :provider provider))))
  (stop [this]
    (if (false? started?)
      this
      (do
        (log/info "Stopping EmailManager")
        (email/shutdown!)
        (assoc this :started? false :provider nil)))))


(defn email-manager [settings]
  (map->EmailManager settings))
