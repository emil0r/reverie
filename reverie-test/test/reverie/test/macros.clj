(ns reverie.test.macros
  (:require [reverie.core :as rev]
            [reverie.system :as sys]
            [midje.sweet :refer :all]))


(fact
 "test deftemplate"
 (rev/deftemplate test-template str)
 (:test-template (:templates @sys/storage)) => truthy)

(fact
 "test defpage"
 (rev/defpage "/foo/bar/baz" {} [["/" {:get (fn [request page params]
                                              "hej")}]])
 (get (:raw-pages @sys/storage) "/foo/bar/baz") => truthy)
