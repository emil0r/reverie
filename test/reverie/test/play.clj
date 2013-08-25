(ns reverie.test.play
  (:require [clout.core :as clout]
            [reverie.atoms :as atoms]
            [reverie.core :as rev]
            [reverie.page :as page]
            [clojure.edn])
  (:use [clojure.pprint :only [pprint]]
        reverie.test.init
        [reverie.util :only [generate-handler]]
        ring.mock.request
        [ring.middleware.edn :only [wrap-edn-params]]
        [ring.middleware.keyword-params :only [wrap-keyword-params]]
        [ring.middleware.params :only [wrap-params]]
        [slingshot.slingshot :only [try+ throw+]]))


;;(println (clojure.edn/read-string "{:asdf 1}"))
(defn test-handler [request]
  {:status 200
   :headers {"Location" "http://localhost"}
   :body "Hello World!"})
(defn ping-handler [request]
  {:status 200
   :headers {"Location" "http://localhost"}
   :body request})


(pprint @atoms/templates)


(let [new-handler (-> ping-handler wrap-edn-params)
         ;; (generate-handler [;;[wrap-params]
         ;;                    [wrap-edn-params]
         ;;                    ] ping-handler)
         req (content-type (request :post "/edn-params" "{:test [1 2 3]}") "application/edn")
         ;;req (request :post "/form-params" {"test" "foo"})
        ;;body (and (edn-request? req) (:body req))
        ;;edn (-read-edn body)
        ]
    ;;(println edn)
    (new-handler req))



;; (let [uris ["/admin" "/admin/api/pages" "/admin/frames/left" "/admin/login" "/admin/logout"]]
;;   (println (reverse (sort-by count (map #(re-find (re-pattern (str "^" %)) "/admin/api/pages/read") uris)))))

;;(reset! atoms/pages {})

;; (rev/defpage "/testus" {}
;;   [:get ["*"] "asdf"]
;;   [:post ["*" {:keys [my] :as datus}] my])

;;(println @atoms/routes)
;;(println "\n--")
;;(clojure.pprint/pprint @atoms/pages)

;; (println (-> (get @atoms/pages "/testus") :fns second second))
;; (clout/route-matches (-> (get @atoms/pages "/testus") :fns second second)
;;                      (request :post "/testus" {"my" "data"}))
;; (println ((-> page/render
;;               wrap-keyword-params
;;               wrap-params)  (request :post "/testus" {:my "data"})) )

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
