(ns reverie.specs.object
  (:require [clojure.spec.alpha :as spec]
            [reverie.object :as object]
            [reverie.specs.database]
            [reverie.specs.field]
            [reverie.specs.i18n]
            [reverie.specs.migration]
            [reverie.specs.renderer]
            [reverie.specs.route]))



(spec/def ::object/disabled? boolean?)
(spec/def ::object/fields (spec/map-of :reverie.field/key :reverie.field/entry))

(spec/def ::object/section map?)
(spec/def ::object/sections (spec/coll-of ::object/section))

(spec/def ::object/i18n :reverie.i18n/dictionary)

;; defobject takes [name options methods]
;; define them here
(spec/def ::object/name symbol?)
(spec/def ::object/options (spec/keys :req-un [:reverie.database/table
                                               :reverie.migration/migration
                                               ::object/fields
                                               ::object/sections]
                                      :opt-un [::object/disabled?
                                               ::object/i18n
                                               :reverie.renderer/renderer]))
(spec/def ::object/methods :reverie.http.route/http-methods)
