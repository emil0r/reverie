(ns reverie.specs.page
  (:require [clojure.spec.alpha :as spec]
            [reverie.specs.route]))

(spec/def :reverie.page/options map?)

