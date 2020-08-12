(ns reverie.test.helpers.middleware)

(defn wrap-tag [handler data]
  (fn [request]
    (let [response (handler request)]
      (update-in response [:tag] conj data))))
