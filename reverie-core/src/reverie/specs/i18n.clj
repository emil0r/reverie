(ns reverie.specs.i18n
  (:require [clojure.spec.alpha :as spec]))

(spec/def :reverie.i18n/dictionary (spec/or :path string?
                                            :data map?))
