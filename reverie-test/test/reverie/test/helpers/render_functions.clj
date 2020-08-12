(ns reverie.test.helpers.render-functions)

(defn get-fn [request page params]
  :get-fn)

(defn get-fn-exception [f]
  (fn [request]
    (f)))

(defn get-fn-simple [request]
  :get-simple-fn)
