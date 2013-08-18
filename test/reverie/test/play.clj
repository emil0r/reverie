(ns reverie.test.play
  (:require [clout.core :as clout]
            [reverie.atoms :as atoms]
            [reverie.core :as rev]
            [reverie.page :as page])
  (:use reverie.test.init
        ring.mock.request
        [ring.middleware.keyword-params :only [wrap-keyword-params]]
        [ring.middleware.params :only [wrap-params]]
        [slingshot.slingshot :only [try+ throw+]]))


;;(reset! atoms/pages {})

(rev/defpage "/testus" {}
  [:get ["*"] "asdf"]
  [:post ["*" {:keys [my] :as datus}] my])

;;(println @atoms/routes)
;;(println "\n--")
(clojure.pprint/pprint @atoms/pages)

;; (println (-> (get @atoms/pages "/testus") :fns second second))
;; (clout/route-matches (-> (get @atoms/pages "/testus") :fns second second)
;;                      (request :post "/testus" {"my" "data"}))
(println ((-> page/render
              wrap-keyword-params
              wrap-params)  (request :post "/testus" {:my "data"})) )

;;(println (request :post "/testus" {"my" "data"}))



;; (defn testus [data]
;;   (try+
;;    (throw+ {:type :test :data :foo})
;;    data
;;    (catch [:type :test] {:keys [data]}
;;      data)))

;; (testus :bar)


;; (defn wrap-t [handler]
;;   (fn [request]
;;     (handler (assoc request :level (+ (:level request) 1)))))

;; ((->
;;   (fn [request]
;;     request)
;;   (wrap-t)
;;   (wrap-t)) {:level 1})
