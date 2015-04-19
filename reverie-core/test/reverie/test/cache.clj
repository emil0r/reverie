(ns reverie.test.cache
  (:require [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [reverie.cache :as cache]
            [reverie.cache.memory :refer [mem-store]]
            [reverie.page :as page]
            [reverie.render :as render]
            [midje.sweet :refer :all]
            [ring.mock.request :refer :all]))


(defrecord TmpPage [data counter options]
  page/IPage
  (serial [this] 1)
  (cache? [this] true)
  (options [this] options)
  (properties [this] options)

  render/IRender
  (render [this request]
    (swap! counter inc)
    (let [qs (:query-string request)]
      (if (str/blank? qs)
        data
        qs))))

(defn get-counter [page]
  @(:counter page))


(fact
 "caching"
 (let [cm (component/start (cache/map->CacheManager {:store (mem-store)}))
       p (TmpPage. "hi" (atom 0) nil)
       r1 (request :get "/")
       r2 (request :get "/" {:foo "bar"})]
   (fact "cache page"
         (cache/cache! cm p "hi" r1)
         (cache/cache! cm p "foo=bar" r2)
         (cache/lookup cm p r1) => "hi")

   (fact "evict page"
         (cache/evict! cm p)
         (cache/lookup cm p r1) => nil)

   (component/stop cm)))
