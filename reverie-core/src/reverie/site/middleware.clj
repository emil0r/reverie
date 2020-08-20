(ns reverie.site.middleware
  "Middleware specific to site"
  (:require [clojure.core.match :refer [match]]
            [reverie.cache :as cache]
            [reverie.http.middleware :refer [wrap-csrf-token]]
            [reverie.http.response :as response]
            [reverie.page :as page]
            [reverie.render :as render]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]))

;; we need these to be dynamic, because site calculates these in the middle of a request
;; and we want to use the middleware pattern for functionality
;; since we also wish to apply various forms of middleware before we hit these middleware functions
;; this leaves us with somehow passing along data, or injecting the middleware
;; of the two passing along via dynamic binding, is maybe not the most elegant solution, but it sidesteps
;; an important issue, namely compiling a chain of middleware functions in order to create the handler function
;; you want for each request. doign it this way that compliation only needs to happen once, instead of dynamically
;; creating it every time. the dynamic binding seems to be a small price to pay in comparison

(def ^:dynamic *cachemanager*)
(def ^:dynamic *page*)
(def ^:dynamic *render-fn*)
(def ^:dynamic *system-pages*)

(defn- get-type [response]
  (cond
    (map? response) :map
    (nil? response) :nil
    :else :default))

(defn wrap-response [handler]
  (fn [request]
    (let [response (handler request)
          body (if (instance? java.io.InputStream (:body response))
                 (:body response)
                 (*render-fn* (:body response)))]
      (match [(get-type response)
              (nil? (get-in response [:headers "Content-Type"]))
              (nil? (:status response))]

             [:map false false]
             (-> response
                 (assoc :body body))

             [:map true false]
             (-> response
                 (assoc-in [:headers "Content-Type"] "text/html; charset=utf-8;")
                 (assoc :body body))

             [:map true true]
             (-> response
                 (assoc-in [:headers "Content-Type"] "text/html; charset=utf-8;")
                 (assoc :status 200)
                 (assoc :body body))

             [:nil _ _]
             (or (get *system-pages* 404)
                 (response/get 404))
             
             [_ _ _]
             {:status 200
              :headers {"Content-Type" "text/html; charset=utf-8;"}
              :body body}))))

(defn wrap-cache [handler]
  (fn [request]
    (let [hit (if (and (page/cache? *page*)
                       (page/published? *page*))
                (cache/lookup *cachemanager* *page* request))]
      (if hit
        ;; we got a hit, render it against the cachemanager
        (*render-fn* (render/render *cachemanager* *page* hit))
        ;; no hit, continue down the chain as per usual
        ;; check if we need to cache the page or not and
        ;; bind the result to the thread local var of *caching?*
        (let [cache? (and (nil? hit)
                          (= (:request-method request) :get)
                          (page/cache? *page*)
                          (page/published? *page*))]
          (if cache?
            ;; we wish to cache the result from the render
            ;; the render
            (binding [cache/*caching?* cache?]
              (let [response (handler request)]
                ;; cache the result
                (cache/cache! *cachemanager* *page* response request)
                ;; give back the response
                response))
            ;; no need to cache the result. go through as normal
            (handler request)))))))


(defn wrap-maybe-forgery [handler]
  (fn [request]
    ;; should we use CSRF protection or not?
    ;; needs to be specifically set to false to remove it
    (if (false? (:forgery? (page/options *page*)))
      (handler request)
      ((wrap-anti-forgery (wrap-csrf-token handler)) request))))

(defn page-handler [request]
  (if-let [handler (page/handler *page* request)]
    (handler request)
    (render/render *page* request)))
