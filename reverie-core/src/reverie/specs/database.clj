(ns reverie.specs.database
  (:require [clojure.spec.alpha :as spec]))

(spec/def :reverie.database/table (spec/or :string string? :keyword keyword?))
