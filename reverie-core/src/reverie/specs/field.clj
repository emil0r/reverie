(ns reverie.specs.field
  (:require [clojure.spec.alpha :as spec]))

(spec/def :reverie.field/key keyword?)
(spec/def :reverie.field.entry/type keyword?)
(spec/def :reverie.field.entry/initial any?)
(spec/def :reverie.field.entry/help string?)
(spec/def :reverie.field/entry (spec/keys :req-un [:reverie.field.entry/type
                                                   :reverie.field.entry/initial]
                                          :opt-un [:reverie.field.entry/help]))
