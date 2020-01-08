(ns reverie.site.apis.api
  (:require [reverie.core :refer [defapi]]
            [schema.core :as s]))

(s/defschema Child {:name s/Str :id s/Int})
(s/defschema Children [Child])

(defonce children (atom {}))

(defn get-children [request page payload params]
  [200 (into [] (vals @children))])

(defn add-child [request page payload params]
  (swap! children assoc (:id payload) payload)
  [201 payload])
(defn get-child [request page payload params]
  (if-let [child (get @children (:id params))]
    [200 child]
    [404 nil]))
(defn update-child [request page payload {:keys [id] :as params}]
  (swap! children assoc id payload)
  [200 payload])

(defapi "/api"
  {:openapi {:info {:title "My Test API"
                    :version "1.0.0"}
             :tags {"child" {:description "Child info"}}
             :spec-path "/docs-spec"}}
  [["/child" {:tags ["child"]
              :methods {:get {:handler get-children
                              :responses {200 {:schema Children}}}
                        :put {:parameters {:body Child}
                              :handler add-child
                              :responses {201 {:schema Child}}}}}
    ["/:id" {:parameters {:path {:id s/Int}}
             :methods {:get {:handler get-child
                             :responses {200 {:schema Child}
                                         404 nil}}
                       :post {:parameters {:body Child}
                              :handler update-child
                              :responses {200 {:schema Child}
                                          404 nil}}}}]]])
