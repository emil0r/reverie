(ns reverie.test.area
  (:require [reverie.core :refer [area]]
            [reverie.page :as page]
            [reverie.render :refer [render]]
            [reverie.template :as template]
            [midje.sweet :refer :all]))

(fact "area can rendered with given rendering"
      (let [t (template/map->Template
               {:function (fn [request page properties params]
                            (area test/area))})]
        (render t {} (page/page {:rendered {:test/area "foo"}})))
      => "foo")
