(ns reverie.test.movement
  (:require [reverie.movement :as m]
            [midje.sweet :refer :all]))


(fact
 "insert origo"
 (fact "empty"
       (m/insert-origo [])
       => [[0 nil]])
 (fact "[[-1 1]]"
       (m/insert-origo [[-1 1]])
       => [[-1 1] [0 nil]])
 (fact "[[1 1]]"
       (m/insert-origo [[1 1]])
       => [[0 nil] [1 1]])
 (fact "[[-2 2] [-1 1]]"
       (m/insert-origo [[-2 2] [-1 1]])
       => [[-2 2] [-1 1] [0 nil]])
 (fact "[[1 1] [2 2]]"
       (m/insert-origo [[1 1] [2 2]])
       => [[0 nil] [1 1] [2 2]])
 (fact "[[-1 1] [1 2] [2 3]]"
       (m/insert-origo [[-1 1] [1 2] [2 3]])
       => [[-1 1] [0 nil] [1 2] [2 3]])
 (fact "[[-2 1] [-1 2] [1 3] [2 4]]"
       (m/insert-origo [[-2 1] [-1 2] [1 3] [2 4]])
       => [[-2 1] [-1 2] [0 nil] [1 3] [2 4]])
 (fact "[[-2 1] [-1 2] [1 3]]"
       (m/insert-origo [[-2 1] [-1 2] [1 3]])
       => [[-2 1] [-1 2] [0 nil] [1 3]])
 (fact "[[-2 1] [2 2]]"
       (m/insert-origo [[-2 1] [2 2]])
       => [[-2 1] [0 nil] [2 2]]))



(fact "move"
      (fact "down simple (3)"
            (m/move [[1 1] [2 2] [3 3] [4 4]] 3 :down)
            => [[1 1] [2 2] [3 4] [4 3]])
      (fact "up simple (3)"
            (m/move [[1 1] [2 2] [3 3] [4 4]] 3 :up)
            => [[1 1] [2 3] [3 2] [4 4]])
      (fact "top simple (3)"
            (m/move [[1 1] [2 2] [3 3] [4 4]] 3 :top)
            => [[1 3] [2 1] [3 2] [4 4]])
      (fact "bottom simple (1)"
            (m/move [[1 1] [2 2] [3 3] [4 4]] 1 :bottom)
            => [[1 2] [2 3] [3 4] [4 1]])
      (fact "down (3)"
            (m/move [[-2 6] [-1 5] [1 1] [2 2] [3 3] [4 4]] 3 :down true)
            => [[-2 6] [-1 5] [0 nil] [1 1] [2 2] [3 4] [4 3]])
      (fact "up (3)"
            (m/move [[-2 6] [-1 5] [1 1] [2 2] [3 3] [4 4]] 3 :up true)
            => [[-2 6] [-1 5] [0 nil] [1 1] [2 3] [3 2] [4 4]])
      (fact "up (1)"
            (m/move [[-2 6] [-1 5] [1 1] [2 2] [3 3] [4 4]] 1 :up true)
            => [[-3 6] [-2 5] [-1 1] [0 nil] [1 2] [2 3] [3 4]])
      (fact "up (1), zero already there"
            (m/move [[-2 6] [-1 5] [0 nil] [1 1] [2 2] [3 3] [4 4]] 1 :up true)
            => [[-3 6] [-2 5] [-1 1] [0 nil] [1 2] [2 3] [3 4]])
      (fact "top (3)"
            (m/move [[-2 6] [-1 5] [1 1] [2 2] [3 3] [4 4]] 3 :top true)
            => [[-3 3] [-2 6] [-1 5] [0 nil] [1 1] [2 2] [3 4]])
      (fact "bottom (1)"
            (m/move [[-1 5] [1 1] [2 2] [3 3] [4 4]] 1 :bottom true)
            => [[-1 5] [0 nil] [1 2] [2 3] [3 4] [4 1]]))


(fact "after"
      (fact "no origo"
            (m/after [[1 20] [2 40] [3 24]] 40 5)
            => [[1 20] [2 40] [3 5] [4 24]])
      (fact "with origo"
            (m/after [[-2 31] [-1 3] [1 20] [2 40] [3 24]] 3 5 true)
            => [[-3 31] [-2 3] [-1 5] [0 nil] [1 20] [2 40] [3 24]]))

(fact "before"
      (fact "no origo"
            (m/before [[1 20] [2 40] [3 24]] 40 5)
            => [[1 20] [2 5] [3 40] [4 24]])
      (fact "with origo"
            (m/before [[-2 31] [-1 3] [1 20] [2 40] [3 24]] 3 5 true)
            => [[-3 31] [-2 5] [-1 3] [0 nil] [1 20] [2 40] [3 24]]))
