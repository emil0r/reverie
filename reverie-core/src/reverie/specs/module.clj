(ns reverie.specs.module
  (:require [clojure.spec.alpha :as spec]
            [reverie.specs.route]))

(spec/def :reverie.module/name symbol?)

