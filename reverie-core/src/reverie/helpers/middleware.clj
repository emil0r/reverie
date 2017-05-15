(ns reverie.helpers.middleware)


(defn create-handler [handlers routes]
  (reduce (fn [current new]
            (if (nil? new)
              current
              (let [[new & args] new]
                (apply new current args))))
          routes
          handlers))

(defn wrap-response-with-handlers-helper []
  (fn [request]
    (get-in request [:reverie :response])))

(defn wrap-response-with-handlers [handlers]
  (create-handler handlers (wrap-response-with-handlers-helper)))
