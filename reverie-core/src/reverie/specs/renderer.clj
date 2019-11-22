(ns reverie.specs.renderer
  (:require [clojure.spec.alpha :as spec]))

(spec/def :reverie.renderer/renderer qualified-keyword?)

