(ns reverie.specs.migration
  (:require [clojure.spec.alpha :as spec]
            [reverie.specs.database]))

(spec/def :reverie.migration/path string?)
(spec/def :reverie.migration/automatic? boolean?)
(spec/def :reverie.migration/migration (spec/keys :req-un [:reverie.migration/path
                                                           :reverie.migration/automatic?]
                                                  :opt-un [:reverie.database/table]))
