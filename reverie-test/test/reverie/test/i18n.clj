(ns reverie.test.i18n
  (:require [com.stuartsierra.component :as component]
            [reverie.i18n :as i18n]
            [midje.sweet :refer :all]))



(fact
 "start component"
 (let [i18n-component (->> {:dictionary {:en {:foo "bar"
                                              :bar "baz is %d"}}}
                           (i18n/get-i18n true)
                           (component/start))]
   (fact
    "get translation"
    (i18n/with-locale :en
      (i18n/t [:foo])) => "bar")

   (fact
    "add translation"
    (i18n/add-i18n! {:dictionary {:en {:baz "my baz"}}})
    (i18n/with-locale :en
      (i18n/t [:baz])))

   (component/stop i18n-component)))
