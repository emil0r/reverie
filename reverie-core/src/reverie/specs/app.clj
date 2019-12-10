(ns reverie.specs.app
  (:require [clojure.spec.alpha :as spec]
            [reverie.specs.route]))


(spec/def :reverie.app/name symbol?)
(spec/def :reverie.app/options map?)

