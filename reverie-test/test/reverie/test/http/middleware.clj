(ns reverie.test.http.middleware
  (:require [reverie.core :refer [defpage]]
            [reverie.helpers.middleware :refer [add-page-middleware
                                                create-handler
                                                merge-handlers]]
            [reverie.http.middleware :refer [authn-exception-handler
                                             authz-exception-handler
                                             default-exception-handler
                                             wrap-authn
                                             wrap-authz
                                             wrap-exceptions]]
            [reverie.http.response :as response]
            [reverie.http.route :as route]
            [reverie.page :as page]
            [reverie.system :as sys]
            [midje.sweet :refer :all]
            [ring.mock.request :refer :all]))


(defn wrap-x [handler x]
  (fn [request]
    (let [response (handler request)]
      (update-in response [:wrap-x] conj x))))

(defn get-fn [request page params]
  :get-fn)

(fact
 "add-page-middleware"
 (fact "merging middleware from options of page to options in routes"
       (let [page (->> {:route (route/route ["/"])
                        :options {:middleware [[wrap-x :wrapped-options]]}
                        :routes (map route/route [["/foo/bar" ^:meta {:middleware [[wrap-x "foo"]
                                                                                   [wrap-x "bar"]]} {:get get-fn}]
                                                  ["/" ;; ^:meta {:middleware [[wrap-x "/"]]}
                                                   {:get get-fn}]])}
                       (page/raw-page)
                       (add-page-middleware))
             handler (-> page :routes first :options :middleware)]
         (:wrap-x (handler (request :get "/foo/bar")))
         => ["bar" "foo" :wrapped-options])))

(fact
 "defpage with middleware"
 (do
   (defpage "/test/http/middleware"
     {:middleware [[wrap-x :test/http/middleware]]}
     [["/" {:get get-fn}]]))
 (let [req (request :get "/test/http/middleware")
       handler (-> (sys/raw-page "/test/http/middleware")
                   (page/handler req))]
   (-> (handler req)
       :wrap-x
       first) => :test/http/middleware))

(defn get-fn-exception [f]
  (fn [request]
    (f)))

(defn get-fn-simple [request]
  :get-simple-fn)

(fact
 "wrap-exceptions"
 (let [exception-handlers {:default default-exception-handler
                           :auth/not-authenticated authn-exception-handler
                           :auth/not-authorized authz-exception-handler}]
   (fact "clojure.lang.ExceptionInfo"
         (let [handler (wrap-exceptions (get-fn-exception #(throw (ex-info "Default handler" {}))) exception-handlers)]
           (handler (request :get "/"))
           => (response/get 500)))
   (fact "java.class.ClassCastException"
         (let [handler (wrap-exceptions (get-fn-exception #(throw "asdf")) exception-handlers)]
           (handler (request :get "/"))
           => (response/get 500)))
   (fact "authn"
         (let [handler (-> get-fn-simple
                           (wrap-authn)
                           (wrap-exceptions exception-handlers))]
           (fact "fail"
                 (let [req (request :get "/")]
                   (handler req))
                 => (response/get 401))
           (fact "success"
                 (let [req (assoc-in (request :get "/") [:reverie :user :id] 1)]
                   (handler req))
                 => :get-simple-fn)))
   (fact "authz"
         (let [handler (-> get-fn-simple
                           (wrap-authz #{:foo})
                           (wrap-exceptions exception-handlers))]
           (fact "fail"
                 (let [req (assoc-in (request :get "/") [:reverie :user :roles] #{:bar})]
                   (handler req))
                 => (response/get 403))
           (fact "success"
                 (let [req (assoc-in (request :get "/") [:reverie :user :roles] #{:foo})]
                   (handler req))
                 => :get-simple-fn))))
 )
