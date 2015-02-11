(ns reverie.test.server
  (:require [com.stuartsierra.component :as component]
            [org.httpkit.client :as client]
            [org.httpkit.server :as http-server :refer [run-server]]
            [noir.session :as session]
            [noir.cookies :as cookies]
            [reverie.test.database.sql-helpers :refer [get-db seed!]]
            reverie.test.helpers
            [reverie.render :as render]
            [reverie.server :as server]
            [reverie.settings :as settings]
            [reverie.site :as site]))


(defn- stop-server [server]
  (when (fn? server)
    (server)))

(let [db (component/start (get-db))
      settings (-> "test/reverie/test/settings.edn"
                   settings/settings
                   component/start)
      site (component/start (site/site
                             {:host-names []
                              :database db
                              :system (:system db)
                              :render-fn (fn [data] (hiccup.compiler/render-html data))}))
      server (component/start
              (reverie.server/get-server {:dev? true
                                          :site site
                                          :server-options (settings/get settings [:server-options])
                                          :run-server run-server
                                          :stop-server stop-server}))]
  (try
    #spy/d @(client/get "http://127.0.0.1:9090/")
    ;; #spy/d (render/render site {:uri "/" :request-method :get})
    (catch Exception e
      (println e)))
  (component/stop db)
  (component/stop settings)
  (component/stop server))
