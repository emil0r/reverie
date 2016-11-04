(ns reverie.site.init
  (:require [clojure.edn :as edn]
            [com.stuartsierra.component :as component]
            [org.httpkit.server :as http-server :refer [run-server]]
            reverie.nsloader
            [reverie.admin :as admin]
            [reverie.admin.api.editors :refer [get-edits-task]]
            [reverie.cache :as cache]
            [reverie.cache.memory :as cache.memory]
            [reverie.cache.sql :as cache.sql]
            [reverie.database.sql :as db.sql]
            [reverie.email :as email]
            [reverie.i18n :as i18n]
            [reverie.logger :as logger]
            [reverie.migrator :as migrator]
            [reverie.migrator.sql :as migrator.sql]
            [reverie.modules.filemanager :as fm]
            [reverie.modules.role :as rm]
            [reverie.page :as page]
            [reverie.redis.core :as redis]
            [reverie.scheduler :as scheduler]
            [reverie.server :as server]
            [reverie.settings :as settings]
            [reverie.site :as site]
            [reverie.system :refer [load-views-ns] :as sys]))


(defn system-map [{:keys [prod? log-settings db-specs ds-specs settings
                          host-names render-fn
                          base-dir media-dirs
                          email-settings
                          cache-store session-store internal-store
                          site-hash-key-strategy
                          server-options middleware-options
                          i18n-tconfig
                          run-server stop-server]}]
  (let [db (component/start (db.sql/database (not prod?) db-specs ds-specs))
        -i18n (component/start (i18n/get-i18n prod? i18n-tconfig))]
    ;; run the migrations
    (->> db
         (migrator.sql/get-migrator)
         (migrator/migrate))

    ;; load the translations for i18n
    (->> -i18n
         (i18n/load-i18n!))

    (component/system-map
     :database db
     :settings settings
     :rolemanager (component/using (rm/get-rolemanager)
                                   [:database])
     :i18n -i18n
     :server (component/using (server/get-server {:server-options server-options
                                                  :run-server run-server
                                                  :stop-server stop-server
                                                  :dev? (not prod?)
                                                  :store session-store})
                              [:filemanager :site])
     :cachemanager (component/using
                    (cache/cachemananger {:store cache-store})
                    [:database])
     :emailmanager (email/email-manager email-settings)
     :filemanager (fm/get-filemanager base-dir media-dirs)
     :site (component/using (site/site {:host-names host-names
                                        :render-fn render-fn})
                            [:database :cachemanager])
     :logger (logger/logger prod? log-settings)
     :scheduler (scheduler/get-scheduler)
     :admin (component/using (admin/get-admin-initializer {:store internal-store})
                             [:database])
     :system (component/using (sys/get-system)
                              [:database :filemanager :site :scheduler
                               :settings :server :logger :emailmanager
                               :admin :cachemanager :i18n]))))


(defonce system (atom nil))

(defn stop []
  (when-not (nil? @system)
    (component/stop @system)
    (reset! system nil)))

(defn- stop-server [server]
  (when (fn? server)
    (server)))

(defn init [settings-path & [{:keys [port primary?]}]]
  ;; read in the settings first
  (let [settings (component/start (settings/settings settings-path))]

    ;; load namespaces after the system starts up
    ;; this step will set up any necessary migrations
    (load-views-ns 'reverie.batteries.objects
                   'reverie.site.templates
                   'reverie.site.apps
                   'reverie.site.endpoints)

    ;; start the system
    (reset! system (component/start
                    (system-map
                     (merge
                      {:cache-store (cache.memory/mem-store)}
                      ;;(redis/get-stores settings)
                      {:prod? (settings/prod? settings)
                       :log-settings (settings/get settings [:log])
                       :email-settings (settings/get settings [:email])
                       :settings settings
                       :i18n-tconfig (settings/get settings [:i18n :tconfig]
                                                   {:dictionary {}
                                                    :dev-mode? (settings/dev? settings)
                                                    :fallback-locale :en})
                       :db-specs (settings/get settings [:db :specs])
                       :ds-specs (settings/get settings [:db :ds-specs])
                       :server-options (merge
                                        (settings/get settings [:server :options])
                                        (if port
                                          {:port port}))
                       :middleware-options (settings/get settings [:server :middleware])
                       :run-server run-server
                       :stop-server stop-server
                       :host-names (settings/get settings [:site :host-names])
                       :render-fn hiccup.compiler/render-html
                       :base-dir (settings/get settings [:filemanager :base-dir])
                       :media-dirs (settings/get settings [:filemanager :media-dirs])}))))


    ;; are we the primary server?
    (when (and (not (false? primary?))
               (settings/get settings [:primary?]))

      ;; start up the scheduler with tasks
      (let [scheduler (-> @system :scheduler)
            cachemanager (-> @system :cachemanager)]
        (scheduler/add-tasks!
         scheduler
         [(get-edits-task
           (settings/get settings [:admin :tasks :edits :minutes]))
          (cache/get-prune-task
           (cache.sql/get-basicstrategy) cachemanager {})])
        (scheduler/start! scheduler)))

    ;; shut down the system if something like ctrl-c is pressed
    (.addShutdownHook
     (Runtime/getRuntime)
     (proxy [Thread] []
       (run []
         (stop))))))
