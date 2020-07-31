(ns reverie.page.util
  (:require [reverie.http.route :as route]))

(defn handle-response [options response]
  (cond
    (map? response) response
    (nil? response) response
    :else
    (let [{:keys [headers status]} (:http options)]
      {:status (or status 200)
       :headers (merge {"Content-Type" "text/html; charset=utf-8;"}
                       headers)
       :body response})))


(defn find-route [request routes]
  (reduce (fn [out route]
            (if (route/match? route request)
              (reduced route)
              nil))
          nil routes))
