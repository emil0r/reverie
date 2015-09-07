(ns reverie.middleware
  (:require [noir.cookies :as cookies]
            [reverie.admin.api.editors :as editors]
            [reverie.auth :as auth]
            [reverie.downstream :refer [*downstream*]]
            [reverie.system :as sys]
            [reverie.response :as response]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [slingshot.slingshot :refer [try+]]
            [taoensso.timbre :as log]))

(defn wrap-downstream
  "Wrap downstream. Used for sending data downstream for later use during a request response cycle."
  [handler]
  (fn [request]
    (binding [*downstream* (atom {})]
      (handler request))))

(defn wrap-admin
  "Wrap admin access"
  [handler]
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

(defn wrap-editor
  "Wrap editor awareness"
  [handler]
  (fn [request]
    (handler
     (assoc-in request
               [:reverie :editor?]
               (editors/editor? (get-in request [:reverie :user]))))))

(defn wrap-error-log
  "Log errors"
  [handler dev?]
  (fn [request]
    (if dev?
      (handler request)
      (try+
       (handler request)
       (catch Object _
         (do
           (let [{:keys [message cause throwable]} &throw-context]
             (log/error {:where ::wrap-error-log
                         :uri (:uri request)
                         :message message
                         :cause cause
                         :stacktrace (if throwable
                                       (log/stacktrace throwable))
                         :request (dissoc request :reverie)}))
           (response/get 500)))))))

(defn wrap-authorized
  "Wrap authorization"
  [handler]
  (fn [request]
    (try+
     (handler request)
     (catch [:type :reverie.auth/not-allowed] {}
       (response/get 401)))))

(defn wrap-reverie-data
  "Add commonly used data from reverie into the request"
  [handler {:keys [dev?]}]
  (fn [{:keys [uri] :as request}]
    (let [data (sys/get-reveriedata)
          user (auth/get-user (:database data))]
      (handler (assoc request :reverie (assoc data :user user))))))

(defn- session-token [request]
  (get-in request [:session :ring.middleware.anti-forgery/anti-forgery-token]))


(defn wrap-csrf-token [handler]
  (fn [request]
    (let [old-token (session-token request)
          x-csrf-token (cookies/get "x-csrf-token" nil)
          response (handler request)]
      (if (= old-token *anti-forgery-token*)
        (if (and (nil? x-csrf-token)
                 (bound? #'*anti-forgery-token*))
          (do (cookies/put! "x-csrf-token" *anti-forgery-token*)
              response)
          response)
        (do
          (cookies/put! "x-csrf-token" *anti-forgery-token*)
          response)))))

(defn wrap-forker [handler & handlers]
  (fn [request]
    (loop [resp (handler request)
           [handler & handlers] handlers]
      (cond
       ;; was the response raised with response/raise-response?
       (= (:type resp) :ring-response)
       (:response resp)

       ;; was the response raised with response/raise?
       (= (:type resp) :response)
       (:response resp)

       ;; was the response nil and do we still have more handlers to try?
       (and (nil? resp) (not (nil? handler)))
       (recur (handler request) handlers)

       ;; end of the line. no more handlers to try
       ;; or it was something other than a 404
       (or (not= 404 (:status resp)) (nil? handler))
       resp

       ;; continue trying
       :else
       (recur (handler request) handlers)))))
