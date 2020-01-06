(ns reverie.page.util
  (:import [reverie RenderException]))

(defn type? [page expected]
  (= (type page) expected))

(defn handle-response [options response]
  (cond
    (map? response) response
    (nil? response) response
    :else
    (let [{:keys [headers status] :or {status 200}} (:http options)]
      {:status status
       :headers (merge {"Content-Type" "text/html; charset=utf-8;"}
                       headers)
       :body response})))

(defn throw-render-exception []
  (throw (RenderException. "[component request sub-component] not implemented for reverie.page/Page")))
