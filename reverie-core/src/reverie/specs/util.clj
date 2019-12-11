(ns reverie.specs.util
  (:require [clojure.spec.alpha :as spec]
            [expound.alpha :as expound]
            [taoensso.timbre :as log]))

(defn assert-spec [spec data]
  (when-not (spec/valid? spec data)
    (let [explanation (expound/expound-str spec data)]
      (log/error explanation)
      (throw (ex-info explanation {:data data})))))
