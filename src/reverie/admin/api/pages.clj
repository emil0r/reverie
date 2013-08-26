(ns reverie.admin.api.pages
  (:require [korma.core :as k]
            [reverie.core :as rev])
  (:use reverie.entity
        [ring.middleware.json :only [wrap-json-params
                                     wrap-json-response]]))

(def test-pages (atom [{:title "Start"
                        :id 1
                        :children [{:title "Test 1"
                                    :id 2}
                                   {:title "Test 2"
                                    :id 3}
                                   {:title "Test 3"
                                    :id 4}
                                   {:title "Test 4"
                                    :id 5
                                    :children [{:title "Test 4.1"
                                                :id 8}
                                               {:title "Test 4.2"
                                                :id 9}
                                               {:title "Test 4.3"
                                                :id 10}]}
                                   {:title "Test 5"
                                    :id 6}
                                   {:title "Test 6"
                                    :id 7}]}]))

(defn- init? []
  (=
   0
   (->
    (k/select page
              (k/aggregate (count :*) :count)
              (k/where {:parent 0 :version 0}))
    first
    :count)))

(defn- status []
  {:should_init (init?)})

(rev/defpage "/admin/api/pages" {:middleware [[wrap-json-params]
                                              [wrap-json-response]]}
  [:get ["/read"]
   @test-pages]
  [:get ["/status"]
   (status)]
  [:post ["/write" data]
   false]
  [:post ["/add" data]
   false]
  [:post ["/delete" data]
   false]
  [:post ["/search" data]
   false]
  [:post ["/publish" data]
   false]
  [:post ["/unpublish" data]
   false])
