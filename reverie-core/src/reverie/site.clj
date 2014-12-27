(ns reverie.site
  (:require [com.stuartsierra.component :as component]
            [reverie.database :as db]
            [reverie.page :as page]
            [reverie.render :as render]
            [reverie.response :as response]
            [slingshot.slingshot :refer [try+]]))


(defprotocol SiteProtocol
  (add-page! [system page])
  (get-page [system request])
  (host-match? [system request])
  (set-system-page! [system status rendered-page])
  (move-object! [system page object area order])
  (move-page! [system page order]))


(defrecord Site [host-names pages system-pages settings database render-fn]
  component/Lifecycle
  (start [this]
    (if pages
      this
      (assoc this
        :pages (atom {}))))
  (stop [this]
    (if-not pages
      this
      (assoc this
        :pages nil
        :system-pages nil)))

  SiteProtocol
  (add-page! [this page]
    (swap! pages assoc (page/path page) page))
  (get-page [this request]
    (let [uri (:uri request)]
      (if-let [page (get @pages uri)]
        page
        (->>
         @pages
         (filter (fn [[k v]]
                   (and (not (page/type? v :page))
                        (re-find (re-pattern (str "^" k)) uri))))
         (sort-by first)
         reverse
         first
         second))))

  (host-match? [this {:keys [server-name]}]
    (if (empty? host-names)
      true
      (some #(= server-name %) host-names)))

  render/RenderProtocol
  (render [this request]
    (try+
     (if-not (host-match? this request)
       (or (get system-pages 404)
           (response/get 404)) ;; no match for against the host names -> 404
       (if-let [p (get-page this request)]
         (if-let [resp (render/render p request)]
           (assoc resp :body (render-fn (:body resp)))
           (or (get system-pages 404)
               (response/get 404))) ;; got back nil -> 404
         (or (get system-pages 404)
             (response/get 404)))) ;; didn't find page -> 404
     (catch [:type :response] {:keys [status args]}
       (or
        (response/get (get system-pages status))
        (apply response/get status args))))))


(defn site [data]
  (map->Site data))
