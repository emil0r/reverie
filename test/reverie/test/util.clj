(ns reverie.test.util)


(defn test-handler [request]
  {:status 200
   :headers {"Location" "http://localhost"}
   :body "Hello World!"})

(defn ping-handler [request]
  {:status 200
   :headers {"Location" "http://localhost"}
   :body request})

(defn wrap-count [handler]
  (fn [request]
    (let [response (handler request)
          level (get-in response [:headers "level"] 1)]
      (assoc-in response [:headers "level"] (+ level 1)))))

(defn wrap-count-obj [handler]
  (fn [request]
    (let [level (get-in request [:headers "level"] 1)]
      (handler (assoc-in request [:headers "level"] (+ level 1))))))
