(ns reverie.meta
  (:require [jayq.core :as jq]
            [jayq.util :as util]))

(def data (atom {}))

(defn read! [f]
  (jq/xhr [:get "/admin/api/meta"] {}
          (fn [json-data]
            (let [edn-data (js->clj json-data :keywordize-keys true)]
              (util/log json-data edn-data)
              (reset! data edn-data)
              (f)))))
