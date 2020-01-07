(ns reverie.site.apis.api
  (:require [reverie.core :refer [defapi]]
            [schema.core :as s]))

(s/defschema Child {:name s/Str :id s/Int})

(defonce children (atom {}))

(defn add-child [request page payload params]
  (swap! children assoc (:id payload) payload))
(defn get-child [request page payload params]
  (get @children (:id params)))
(defn update-child [request page payload {:keys [id] :as params}]
  (swap! children update-in [id] payload))

(defapi "/api"
  {:openapi {:info {:title "My Test API"
                    :version "1.0.0"}
             :tags {"child" {:description "Child info"}}
             :spec-path "/docs-spec"}}
  [["/child" {:tags ["child"]
              :methods {:put {:parameters {:body Child}
                              :handler add-child
                              :responses {200 {:schema Child}}}}}
    ["/:id" {:parameters {:path {:id s/Int}}
             :methods {:get {:handler get-child
                             :responses {200 {:schema Child}}}
                       :post {:parameters {:body Child}
                              :handler update-child
                              :responses {200 {:schema Child}
                                          404 {}}}}}]]])

