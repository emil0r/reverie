(ns reverie.communication
  (:require [ajax.core :as ajax]
            [ajax.interceptors :as ajax-interceptors]
            [ajax.transit :as ajax-transit]
            [com.stuartsierra.component :as component]
            [clojure.string :as str]
            [cognitect.transit :as transit]
            [java.time]
            [re-frame.core :as rf]
            [reverie.communication.records :as records]
            [taoensso.timbre :as log]
            [tick.alpha.api :as t]))

(defn get-cookie [cookie-name]
  ;; check that cookie does exist
  (if (and (.-cookie js/document) (not= "" (.-cookie js/document)))
    ;; split cookie into all the cookies
    (let [cookies (str/split (.-cookie js/document) ";")]
      ;; reduce over the cookies until we find the one we want. could do a loop, but there
      ;; will never be enough cookies to make the extra effort worth it
      (reduce (fn [out cookie]
                (if (= (str cookie-name "=") (.substring cookie 0 (inc (count cookie-name))))
                  (js/decodeURIComponent (.substring cookie (inc (count cookie-name))))
                  out))
              nil (->> cookies
                       (remove nil?)
                       (map str/trim))))
    ;; return nil if we found nothing
    nil))

(defn csrf-token-cookie [headers]
  (if-let [token (get-cookie "x-csrf-token")]
    (assoc headers "x-csrf-token" token)
    headers))

(defn massage-headers [headers]
  (-> headers
      csrf-token-cookie))


(def credentials (atom nil))
(def base-url (atom nil))

(defn time-fn [obj]
  (str obj))
(defn rep [text]
  (fn [& _]
    text))

(defn <-user [data]
  (records/map->User data))
(defn ->user [data]
  (pr-str (select-keys data [:id :username :email :active?
                             :created :last-login
                             :spoken-name :full-name
                             :roles :groups])))

(def write-handlers {records/User (transit/write-handler "reverie.auth.User" ->user)
                     java.time/Instant (transit/write-handler (rep "time/instant") time-fn)
                     java.time/Month (transit/write-handler (rep "time/month") time-fn)
                     java.time/DayOfWeek (transit/write-handler (rep "time/day-of-week") time-fn)
                     java.time/Year (transit/write-handler (rep "time/year") time-fn)})
(def writer (transit/writer :json {:handlers write-handlers}))
(def read-handlers {"reverie.auth.User" (transit/read-handler <-user)
                    "time/instant" (transit/read-handler (fn [obj] (t/instant obj)))
                    "time/month" (transit/read-handler (fn [obj] (t/month obj)))
                    "time/day-of-week" (transit/read-handler (fn [obj] (t/day-of-week obj)))
                    "time/year" (transit/read-handler (fn [obj] (t/year obj)))})
(def response-format (ajax-interceptors/map->ResponseFormat
                      {:content-type ["application/transit+json"]
                       :description "Transit response"
                       :read (ajax-transit/transit-read-fn {:handlers read-handlers})}))


(defn- encode-get-params [params]
  (reduce (fn [out [k v]]
            (if (keyword? k)
              (let [k (-> k
                          str
                          (subs 1)
                          (js/encodeURIComponent))]
                (assoc out k v))
              (assoc out k v)))
          {} params))

(defn- get-handler+error-handler [chained]
  (cond (vector? chained)
        [(fn [data]
           (rf/dispatch (into chained data)))
         nil]
        (map? chained)
        ((juxt :handler :error-handler) chained)

        :else
        [(fn [data]
           (rf/dispatch [chained data]))
         nil])
  )

(defn- event-fx-request-map [params chained]
  (let [[handler error-handler] (get-handler+error-handler chained)]
   (update
    (merge {:params params
            :writer writer
            :with-credentials true
            :response-format response-format}
           (when handler
             {:handler handler})
           (when error-handler
             {:error-handler error-handler}))
    :headers massage-headers @credentials)))

(defn- manual-request-map [params handler error-handler]
  (update
   (merge {:params params
           :writer writer
           :with-credentials true
           :response-format response-format}
          (when handler
            {:handler handler})
          (when error-handler
            {:error-handler error-handler}))
   :headers massage-headers @credentials))

(defn GET
  ([uri params] (GET uri params nil nil))
  ([uri params handler] (GET uri params handler nil))
  ([uri params handler error-handler]
   (ajax/GET (str @base-url uri)
             (manual-request-map params handler error-handler))))

(defn POST
  ([uri params] (POST uri params nil nil))
  ([uri params handler] (POST uri params handler nil))
  ([uri params handler error-handler]
   (ajax/POST (str @base-url uri)
              (manual-request-map params handler error-handler))))

(defn PUT
  ([uri params] (PUT uri params nil nil))
  ([uri params handler] (PUT uri params handler nil))
  ([uri params handler error-handler]
   (ajax/PUT (str @base-url uri)
             (manual-request-map params handler error-handler))))

(defn DELETE
  ([uri params] (DELETE uri params nil nil))
  ([uri params handler] (DELETE uri params handler nil))
  ([uri params handler error-handler]
   (ajax/DELETE (str @base-url uri)
                (manual-request-map params handler error-handler))))

(rf/reg-event-fx :http/get (fn [_ [_ uri params chained]]
                             (ajax/GET (str @base-url uri)
                                       (event-fx-request-map (encode-get-params params) chained))
                             nil))

(rf/reg-event-fx :http/post (fn [_ [_ uri params chained]]
                              (ajax/POST (str @base-url uri)
                                         (event-fx-request-map params chained))
                              nil))

(rf/reg-event-fx :http/put (fn [_ [_ uri params chained]]
                             (ajax/PUT (str @base-url uri)
                                       (event-fx-request-map params chained))
                             nil))

(rf/reg-event-fx :http/delete (fn [_ [_ uri params chained]]
                                (ajax/DELETE (str @base-url uri)
                                             (event-fx-request-map params chained))
                                nil))


(defrecord CommunicationManager [started? url]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (log/info "Starting CommunicationManager")
          (log/info "Setting Communication base-url to " url)
          (reset! base-url url)
          (assoc this
                 :started? true))))
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping CommunicationManager")
          (reset! base-url nil)
          (assoc this
                 :started? false)))))

(defn communication-manager [settings]
  (map->CommunicationManager settings))
