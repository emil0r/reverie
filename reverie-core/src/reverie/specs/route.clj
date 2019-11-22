(ns reverie.specs.route
  (:require [clojure.spec.alpha :as spec]))

(spec/def :reverie.route/clout-string string?)

(spec/def :reverie.route/http-methods
  (spec/map-of #{:any :get :post :put :delete :head :options}
               (spec/or :fn fn?
                        :symbol symbol?)))
