(ns reverie.test.app
  (:require [reverie.core :as rev]
            [reverie.page :as page])
  (:use midje.sweet
        ring.mock.request))


(rev/defapp gallery {}
  ;; test :get
  [:get ["/:gallery/:image" {:gallery #"\w+" :image #"\d+"}] (clojure.string/join "/" [gallery image])]
  [:get ["/:gallery" {:gallery #"\w+"} {:wrap [nil]}] (str "this is my " gallery)]
  [:get ["*"] "base"]
  ;; test order in methods array
  [:post ["/:gallery" {:gallery #"\w+"} data] (str gallery ", my post -> " data)]
  [:post ["/:gallery" {:gallery #"\w+"} {:wrap [nil]} data] (str gallery ", my post -> " data)]
  [:post ["/:gallery" {:wrap [nil]} data] (str gallery ", my post -> " data)]
  [:post ["*" data] (str "my post -> " data)]
  ;; deconstructing works
  [:post ["*" {:keys [testus] :as data}] (= testus true)])


(page/add! {:tx-data {:uri "/gallery" :name "Gallery" :title "Gallery"
                      :template :main :parent 0 :order 0 :type :app
                      :app :gallery}})

(fact
 "page-render with app"
 (let [req (request :get "/gallery/garden/1")]
   (:body (page/render req))) => "garden/1")

;; (rev/defpage "/testus" {}
;;   [:get ["/:foo/:bar"] (str foo "/" bar)]
;;   [:get ["*"] "my test page"])
