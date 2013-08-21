(ns reverie.test.core
  (:require [me.raynes.fs :as fs]
            [hiccup.core :as hiccup]
            [korma.core :as korma]
            [reverie.core :as rev])
  (:use midje.sweet
        reverie.atoms
        reverie.test.init
        ring.mock.request))




(defn reset-routes! []
  (reset! routes {}))

(defn reset-templates! []
  (reset! templates {}))

(defn reset-objects! []
  (reset! objects {}))


(fact
 "deftemplate"
 (do
   (reset-templates!)
   (rev/deftemplate main {:areas [:a :b :c]} "body")
   (let [m @templates
         k (first (keys m))
         options (:options (m k))
         func (:fn (m k))]
     [k options (:body (func {:request (request :get "/")
                              :connection nil
                              :page-id nil
                              :mode :edit}))])) => [:main
                                                    {:areas [:a :b :c]}
                                                    "body"])


(fact
 "objectfuncs simple"
 (let [obj (rev/object-funcs [] [:get :post] (clojure.string/join " " ["this" "is" "my" "function!"]))]
   [((:get obj) :data-placeholder {})
    ((:post obj) :data-placeholder {})]) =>
    ["this is my function!"
     "this is my function!"])

(fact
 "objectfuncs multiple method/fn"
 (let [obj (rev/object-funcs [] [:get fn-get :post fn-post]
                             [fn-get "my get"]
                             [fn-post "my post"])]
   [((:get obj) :data-placeholder {}) ((:post obj) :data-placeholder {})]) => ["my get" "my post"])

(fact
 "objectfuncs attributes"
 (let [obj (rev/object-funcs [text] [:get] (hiccup/html [:div "this is my " text]))]
   ((:get obj) :data-placeholder {:text "text"})) => "<div>this is my text</div>")


(fact
 "defobject"
 (do
   (reset-objects!)
   (rev/defobject text {:areas [:a :b]
                        :table :test_text
                        :attributes {:text {:initial ""
                                            :input :text
                                            :name "Text"}}}
     [:get]
     "")
   (-> @objects :text nil?)) => false)

(fact
 "defobject and atttributes"
 (reset-objects!)
 (rev/defobject text {:areas [:a :b]
                      :table :test_text
                      :attributes {:text {:initial "" :input :text :name "Text" :description ""}}} [:get] text)
 (let [f (-> @objects :text :get)]
   (f {:uri "/"} {:text "my text"})) => "my text")

(fact
 "defobject and korma"
 (reset-objects!)
 (rev/defobject text {:areas [:a :b]
                      :table :test_text
                      :attributes {:text {:initial "" :input :text :name "Text" :description ""}}} [:get] text)
 (korma/as-sql (korma/select* (-> @objects :text :entity)))
 => "SELECT \"test_text\".* FROM \"test_text\"")

(fact
 "routes"
 (reset-routes!)
 (add-route! "/" {:page-id 1 :type :normal})
 (add-route! "/info" {:page-id 2 :type :normal})
 (add-route! "/contact" {:page-id 3 :type :page})
 (add-route! "/apps/app1" {:page-id 4 :type :app})
 (add-route! "/apps/app2" {:page-id 5 :type :app})
 (map #(-> % second :page-id) [(get-route "/")
                               (get-route "/contact")
                               (get-route "/info")
                               (get-route "/apps/app1")
                               (get-route "/apps/app2/asdf")
                               (get-route "/contact/ceo")
                               (get-route "/info/does-not-exist")])
 => [1 3 2 4 5 3 nil])

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
  [:post ["*" {:keys [testus] :as data}] (= testus true)]
  [:post ["*" data] (rev/raise-response {:status 404 :body "404, page not found"})]
  )

(fact
 "defapp"
 (let [app (:gallery @apps)
       [[_ _ _ g1] [_ _ _ g2] [_ _ _ g3]
        [_ _ _ p1] [_ _ _ p2] [_ _ _ p3] [_ _ _ p4] [_ _ _ p5] [_ _ _ p6]] (:fns app)]
   [(:body (g1 {} {:gallery "gallery" :image "image"}))
    (:body (g2 {} {:gallery "gallery"}))
    (:body (g3 {} {}))
    (:body (p1 {} {:gallery "gallery"} "my data"))
    (:body (p2 {} {:gallery "gallery2"} "my data"))
    (:body (p3 {} {:gallery "gallery3"} "my data"))
    (:body (p4 {} {} "my data here"))
    (:body (p5 {} {} {:testus true}))
    (p6 {} {} {:not :this})
    ])
 => ["gallery/image"
     "this is my gallery"
     "base"
     "gallery, my post -> my data"
     "gallery2, my post -> my data"
     "gallery3, my post -> my data"
     "my post -> my data here"
     true
     {:status 404 :body "404, page not found"}
     ])


(rev/defpage "/test/defpage" {}
  [:get ["/:foo/:bar"] (str foo "/" bar)]
  [:get ["/"] "asdf"]
  ;;[:post ["*"] "posted asdf"]
  )


(fact
 "defpage"
 (:body (reverie.page/render (request :get "/test/defpage/foo/bar"))) => "foo/bar")

(fact
 "defpage 404"
 (:status (reverie.page/render (request :get "/test/defpage/no-no"))) => 404)

(fact
 "defpage /"
 (:body (reverie.page/render (request :get "/test/defpage"))) => "asdf")



