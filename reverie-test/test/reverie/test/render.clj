(ns reverie.test.render
  (:require [com.stuartsierra.component :as component]
            [hiccup.core :as hiccup]
            [reverie.test.dummy :as dummy]
            [reverie.area :as area]
            [reverie.core :as rev]
            [reverie.database :as db]
            [reverie.object :as object]
            [reverie.page :as page]
            [reverie.render :as render]
            [reverie.route :as route]
            [reverie.site :as site]
            [reverie.system :as sys]
            [reverie.template :as template]
            [ring.mock.request :refer :all]
            [midje.sweet :refer :all]))


(defn- template-render [request page properties params]
  [:html
   [:head
    [:meta {:charset "UTF-8"}]
    [:title (page/title page)]]
   [:body
    [:div
     (rev/area a)]]])

(defn- app-route-base [request page properties params]
  {:a [:p "app route base"]})
(defn- app-route-foobar [request page properties {:keys [bar]}]
  {:a (list [:p "app route foobar"]
            [:p "bar is " bar])})

(defn- object-text-render [request object properties params]
  [:p (:text properties)])

(defn object-image-render [request object properties params]
  [:img {:src (:src properties) :alt (:alt properties) :title (:title properties)}])

(defn first-page []
  (let [t (template/template template-render)
        p (page/page {:template t :id 1 :name "Test page"
                      :route "/"
                      :title "My test page" :parent nil :children nil
                      :properties {}})
        objects [(object/object {:id 1 :name "Text" :area :a :page p
                                 :order -1
                                 :properties {:text "My text"}
                                 :methods {:any object-text-render}})
                 (object/object {:id 2 :name "Image" :area :a :page p
                                 :order 1
                                 :properties {:src "/images/test.png"
                                                    :alt "Alt"
                                                    :title "Title"}
                                 :methods {:get object-image-render}})]
        p (assoc p :objects objects)]
    p))

(defn first-page-app []
  (let [t (template/template template-render)
        p (page/app-page {:template t :id 1 :name "Test page"
                          :title "My test page" :parent nil :children nil
                          :route "/"
                          :properties {}
                          :app :my-test-app
                          :app-routes [(route/route ["/" {:get app-route-base}])
                                       (route/route ["/foo/:bar" {:get app-route-foobar}])]})
        objects [(object/object {:id 1 :name "Text" :area :a :page p
                                 :order -1
                                 :properties {:text "My text"}
                                 :methods {:any object-text-render}})
                 (object/object {:id 2 :name "Image" :area :a :page p
                                 :order 1
                                 :properties {:src "/images/test.png"
                                                    :alt "Alt"
                                                    :title "Title"}
                                 :methods {:get object-image-render}})]
        p (assoc p :objects objects)]
    p))

(defn expected-result
  ([]
     (expected-result nil))
  ([middle]
     (hiccup/html
      [:html
       [:head
        [:meta {:charset "UTF-8"}]
        [:title "My test page"]]
       [:body
        [:div
         [:p "My text"]
         middle
         [:img {:alt "Alt" :src "/images/test.png" :title "Title"}]]]])))

(fact
 "rendering with :page, manually setup"
 (let [req (request :get "/")
       p (first-page)]
   (hiccup/html (:body (render/render p req))))
 => (expected-result))


(fact
 "rendering with :app, manually setup"
 (let [req (request :get "/")
       p (first-page-app)]
   (hiccup/html (:body (render/render p req))))
 => (expected-result [:p "app route base"]))

;; (fact
;;  "rendering with site/Site"
;;  (let [dummy-db (dummy/get-db)
;;        dummy-sys (dummy/get-system)
;;        s (component/start (site/site {:host-names ["example.com"]
;;                                       :database dummy-db
;;                                       :system dummy-sys
;;                                       :render-fn (fn [data] (hiccup.compiler/render-html data))}))]
;;    (reset! sys/storage {})
;;    (sys/add-template-type! dummy-sys
;;                            :testus (template/template template-render))
;;    (sys/add-app-type! dummy-sys
;;                       :foobar {:properties {}
;;                                :app-routes [(route/route ["/" {:get app-route-base}])
;;                                             (route/route ["/:bar" {:get app-route-foobar}])]})
;;    (sys/add-object-type! dummy-sys
;;                          :text {:properties {:name :text :etc :...}
;;                                 :methods {:any object-text-render}})
;;    (sys/add-object-type! dummy-sys
;;                          :image {:properties {:name :text :etc :...}
;;                                  :methods {:get object-image-render}})
;;    (fact "get page"
;;          (page/id (site/get-page s {:uri "/"})) => 1)
;;    (fact "get page /foo/bar"
;;          (page/id (site/get-page s {:uri "/foo/bar"})) => 2)
;;    (fact "404 due to wrong host name"
;;          (:status (render/render s {:uri "/"})) => 404)
;;    (fact "rendered page, uri /"
;;          (:body
;;           (render/render s {:uri "/" :server-name "example.com"
;;                             :request-method :get}))
;;          => (expected-result))
;;    (fact "rendered page, uri /foo/bar"
;;          (:body
;;           (render/render s {:uri "/foo/bar" :server-name "example.com"
;;                             :request-method :get}))
;;          => (expected-result (list [:p "app route foobar"]
;;                                    [:p "bar is bar"])))
;;    (fact "404 due to wrong path"
;;          (:status
;;           (render/render s {:uri "/foo/bar/baz" :server-name "example.com"
;;                             :request-method :get}))
;;          => 404)))
