(ns reverie.specs.object
  (:require [clojure.spec.alpha :as spec]
            [reverie.object :as object]))


;; everything underneath here has to do with options
(spec/def :reverie.database/table (spec/or :string string? :keyword keyword?))

(spec/def :reverie.migration/path string?)
(spec/def :reverie.migration/automatic? boolean?)
(spec/def :reverie.migration/migration (spec/keys :req-un [:reverie.migration/path
                                                           :reverie.migration/automatic?]
                                                  :opt-un [:reverie.database/table]))

(spec/def ::object/disabled? boolean?)
(spec/def :reverie.field/key keyword?)
(spec/def :reverie.field.entry/type keyword?)
(spec/def :reverie.field.entry/initial any?)
(spec/def :reverie.field.entry/help string?)
(spec/def :reverie.field/entry (spec/keys :req-un [:reverie.field.entry/type
                                                   :reverie.field.entry/initial]
                                          :opt-un [:reverie.field.entry/help]))
(spec/def ::object/fields (spec/map-of :reverie.field/key :reverie.field/entry))

(spec/def ::object/section map?)
(spec/def ::object/sections (spec/coll-of ::object/section))

(spec/def :reverie.renderer/renderer qualified-keyword?)

(spec/def :reverie.i18n/dictionary (spec/or :path string?
                                            :data map?))
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
(spec/def ::object/methods (spec/map-of #{:any :get :post :put :delete :head :options}
                                        (spec/or :fn fn?
                                                 :symbol symbol?)))
