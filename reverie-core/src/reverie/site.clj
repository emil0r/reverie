(ns reverie.site
  (:require [com.stuartsierra.component :as component]
            [reverie.auth :as auth]
            [reverie.cache :as cache]
            [reverie.helpers.middleware :refer [create-handler]]
            [reverie.http.response :as response]
            [reverie.http.route :as route]
            [reverie.http.router :as router]
            [reverie.page :as page]
            [reverie.render :as render]
            [reverie.site.middleware :refer [*cachemanager*
                                             *page*
                                             *render-fn*
                                             *system-pages*
                                             page-handler
                                             wrap-cache
                                             wrap-maybe-forgery
                                             wrap-response]]
            [slingshot.slingshot :refer [try+]]
            [taoensso.timbre :as log])
  (:import [reverie RenderException]))

(defprotocol ISite
  (get-host [site request]))

(defrecord Site [started? hosts cachemanager system-pages render-fn]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (log/info "Starting Site" {:hosts (vec (keys hosts))})
          (assoc this
                 :started? true
                 :hosts (->> hosts
                             (map (fn [[k router]]
                                    (let [middleware (->> (into [[wrap-maybe-forgery]
                                                                 [wrap-cache]
                                                                 [wrap-response]] (:middleware router))
                                                          (remove nil?)
                                                          (vec))]
                                      [k {:router router :handler (create-handler middleware page-handler)}])))
                             (into {}))))))
  (stop [this]
    (if-not started?
      this
      (do
        (log/info "Stopping Site" {:hosts (vec (keys (:hosts this)))})
        (assoc this
               :hosts {}
               :started? false))))

  ISite
  (get-host [this {:keys [server-name]}]
    (or (get hosts server-name)
        (get hosts "*")))

  render/IRender
  (render [this request]
    (try+
     (let [host (get-host this request)
           {:keys [router handler]} host
           page (router/get-page router request)
           user (auth/get-user request)]
       (cond
         ;; no host found
         (nil? router) 
         (or (get system-pages 404)
             (response/get 404))

         ;; page found
         page
         (binding [*cachemanager* cachemanager
                   *page* page
                   *render-fn* render-fn
                   *system-pages* system-pages]
           (handler request))

         :else
         (or (get system-pages 404)
             (response/get 404))))
     (catch [:type :ring-response] out
       out)
     (catch [:type :response] {:keys [status args]}
       {:type :response
        :response (or
                   (get system-pages status)
                   (apply response/get status args))})))
  (render [this _ _]
    (throw (RenderException. "[component request sub-component] not implemented for reverie.site/Site"))))


(defn get-site [data]
  (map->Site data))
