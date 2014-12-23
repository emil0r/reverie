(ns reverie.site
  (:require [com.stuartsierra.component :as component]
            [reverie.database :as db]
            [reverie.page :as page]
            [reverie.render :as render]))


(defprotocol SiteProtocol
  (add-page! [system page])
  (get-page [system request])
  (host-match? [system request])
  (set-system-page! [system status rendered-page])
  (move-object! [system page object area order])
  (move-page! [system page order]))


(defrecord Site [host-names pages system-pages settings database]
  component/Lifecycle
  (start [this]
    (if pages
      this
      (assoc this
        :pages (atom {})
        :system-pages (atom {404 {:status 404 :headers {} :body "404, Page Not Found"}
                             500 {:status 500 :headers {} :body "500, Internal Server Error"}}))))
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
    (if-not (host-match? this request)
      (get @system-pages 404) ;; no match for against the host names -> 404
      (if-let [p (get-page this request)]
        (if-let [resp (render/render p request)]
          resp
          (get @system-pages 404)) ;; got back nil -> 404
        (get @system-pages 404))))) ;; didn't find page -> 404


(defn site [data]
  (map->Site data))
