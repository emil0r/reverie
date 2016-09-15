(ns reverie.test.page
  (:require
   [clj-time.core :as t]
   [reverie.page :as page]
   [reverie.route :as route]
   [reverie.render :as render]
   [reverie.system :as sys]
   [midje.sweet :refer :all]))


(defn get-renderer
  ([]
   (get-renderer nil))
  ([methods]
   (render/map->Renderer {:name ::renderer :options {:render-fn :hiccup} :methods methods})))


(defn get-app-page [routes]
  (page/map->AppPage {:route nil
                      :app "app"
                      :app-routes []
                      :app-area-mappings nil
                      :slug "/"
                      :id 1
                      :serial 1
                      :name "Test"
                      :title ""
                      :properties {}
                      :options {}
                      :template :template
                      :created (t/now)
                      :updated (t/now)
                      :parent nil
                      :data nil
                      :version 0
                      :published-data (t/now)
                      :published? true
                      :objects []
                      :raw-data nil}))
