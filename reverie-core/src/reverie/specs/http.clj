(ns reverie.specs.http
  (:require [clojure.spec.alpha :as spec]))


(spec/def :http.response/status int?)
(spec/def :http.response/headers map?)
(spec/def :http.response/body (spec/or :string string?))

(spec/def :http/response (spec/keys :req-un [:http.response/status
                                             :http.response/headers
                                             :http.response/body]))
