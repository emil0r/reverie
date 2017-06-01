(ns reverie.middleware
  (:require [clojure.core.memoize :as memo]
            [clojure.string :as str]
            [noir.cookies :as cookies]
            [noir.session :as session]
            [reverie.admin.api.editors :as editors]
            [reverie.auth :as auth]
            [reverie.downstream :refer [*downstream*]]
            [reverie.i18n :as i18n]
            [reverie.system :as sys]
            [reverie.response :as response]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [slingshot.slingshot :refer [try+]]
            [taoensso.timbre :as log]
            [taoensso.tower.utils :as tower.utils]))

(defn wrap-downstream
  "Wrap downstream. Used for sending data downstream for later use during a request response cycle."
  [handler]
  (fn [request]
    (binding [*downstream* (atom {})]
      (handler request))))

(defn wrap-admin
  "Wrap admin access"
  [handler]
  (fn [{:keys [^String uri] :as request}]
    (if (and
         (.startsWith uri "/admin")
         (not (.startsWith uri "/admin/log")))
      (try+
       (handler request)
       (catch [:type :reverie.auth/not-allowed] {}
         (log/info "Unauthorized request for admin area"
                   {:user-id (auth/get-id)
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
    (let [user (auth/get-user (:database sys/reverie-data))]
      (handler (assoc request :reverie (assoc sys/reverie-data :user user :dev? dev?))))))

(defn- session-token [request]
  (get-in request [:session :ring.middleware.anti-forgery/anti-forgery-token]))


(defn wrap-csrf-token [handler]
  (fn [request]
    (let [old-token (session-token request)
          x-csrf-token (cookies/get "x-csrf-token" nil)
          response (handler request)]
      (if (= old-token *anti-forgery-token*)
        (cond
         ;; no x-csrf-token found in the inbound cookie
         (nil? x-csrf-token)
         (do (cookies/put! "x-csrf-token" *anti-forgery-token*)
             response)
         ;; x-csrf-token from the cookie does not equal the
         ;; one we got from the wrap-anti-forgery middleware
         ;; NOTE: we should only hit this during GET, HEAD and OPTIONS
         ;; the rest will be blocked by the wrap-anti-forgery middleware
         (not= x-csrf-token old-token)
         (do (cookies/put! "x-csrf-token" *anti-forgery-token*)
             response)
         ;; all is good
         :else
          response)
        (do
          (cookies/put! "x-csrf-token" *anti-forgery-token*)
          response)))))

(defn- get-locales* [headers-accept-language
                     {:keys [only-locale enforce-locale preferred-locale fallback-locale] :as opts}
                     session-locale]
  (if only-locale
    only-locale
    (let [ ;; ["en-GB" "en" "en-US"], etc.
          accept-lang-locales (->> headers-accept-language
                                   (tower.utils/parse-http-accept-header)
                                   (mapv (fn [[l q]] l)))]
      (->> [enforce-locale
            session-locale
            preferred-locale
            accept-lang-locales
            (or fallback-locale :en)]
           flatten
           (remove nil?)
           (into [])))))

;; minor speed boost
(def get-locales (memo/lru get-locales* :lru/threshold 50))

(defn wrap-i18n
  "Borrows bits and pieces from tower's wrap-tower"
  [handler {:keys [enforce-locale preferred-locale fallback-locale] :as opts}]
  (fn [{:keys [headers] :as request}]
    (binding [i18n/*locale* (get-locales (get headers "accept-language") opts (session/get :locale nil))]
      (handler request))))

;; TODO: look at this again. potentially the server does a lot of
;; unnecessary work
(defn wrap-forker [handler & handlers]
  (fn [request]
    (loop [resp (handler request)
           saved-resp resp
           [handler & handlers] handlers]
      (cond
        (and (map? resp)
             (contains? resp :body)
             (not (contains? resp :status)))
        resp

        (and (map? resp)
             (contains? resp :status)
             (not= 404 (:status resp)))
        resp

        ;; was the response raised with response/raise-response?
        (= (:type resp) :ring-response)
        (:response resp)

        ;; was the response raised with response/raise and it's not a 404?
        (and (= (:type resp) :response)
             (not= 404 (get-in resp [:response :status])))
        (:response resp)

        ;; was the response nil and do we still have more handlers to try?
        (and (nil? resp) (not (nil? handler)))
        (recur (handler request) saved-resp handlers)

        ;; end of the line. no more handlers to try
        ;; or it was something other than a 404
        (or (and
              (not (nil? (get-in resp [:response :status])))
              (not= 404 (get-in resp [:response :status])))
            (and
              (not (nil? (get-in resp [:status])))
              (not= 404 (get-in resp [:status])))
            (nil? handler))
        ;; resp can be from reverie.site or from ring middleware.
        ;; reverie.site always gives back map with keys :type and :response
        ;; whereas ring middleware just gives back the map that reverie.site
        ;; places in :response
        (or (:response resp) resp (:response saved-resp) saved-resp)

        ;; continue trying
        :else
        (recur (handler request) saved-resp handlers)))))

(defn wrap-resources
  "Check for resources being used based on URI"
  [handler routes]
  (fn [{:keys [uri] :as request}]
    (if-let [new-handler (reduce (fn [out [paths handler]]
                                   (if (some #(str/starts-with? uri %) paths)
                                     handler
                                     out))
                                 nil routes)]
      (new-handler request)
      (handler request))))
