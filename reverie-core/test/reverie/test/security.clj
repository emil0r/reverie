(ns reverie.test.security
  (:require [reverie.security :refer [with-access]]
            [midje.sweet :refer :all]))



(fact "nil nil"
      (with-access nil nil true)
      => true)
