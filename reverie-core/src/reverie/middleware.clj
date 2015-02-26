(ns reverie.middleware
  (:require [reverie.auth :as auth]
            [reverie.system :as sys]
            [reverie.response :as response]
            [slingshot.slingshot :refer [try+]]
            [taoensso.timbre :as log]))


(defn wrap-admin [handler]
  (fn [{:keys [uri] :as request}]
    (if (and
         (re-find #"^/admin" uri)
         (not (re-find #"^/admin/login" uri))
         (not (re-find #"^/admin/logout" uri)))
      (try+
       (handler request)
       (catch [:type :reverie.auth/not-allowed] {}
         (log/info "Unauthorized request for admin area"
                   {:user (get-in request [:reverie :user])
                    :request (select-keys request [:headers
                                                   :remote-address
                                                   :uri])})
         (response/get 302 "/admin/login")))
      (handler request))))

(defn wrap-error-log [handler dev?]
  (fn [request]
    (if dev?
      (handler request)
      (try
        (handler request)
        (catch Exception e
          (do
            (log/error "Caught an exception" e)
            (response/get 500)))))))

(defn wrap-authorized [handler]
  (fn [request]
    (try+
     (handler request)
     (catch [:type :reverie.auth/not-allowed] {}
       (response/get 401)))))

(defn wrap-reverie-data [handler]
  (fn [{:keys [uri] :as request}]
    (let [db (sys/get-db)
          user (auth/get-user db)]
      (handler (assoc request
                 :reverie {:user user
                           :database db})))))


(defn wrap-forker [handler & handlers]
  (fn [request]
    (loop [resp (handler request)
           [handler & handlers] handlers]
      (if (or (not= 404 (:status resp))
              (nil? handler))
        resp
        (recur (handler request) handlers)))))
