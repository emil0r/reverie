(ns reverie.admin.api
  (:require [korma.core :as k]
            [reverie.atoms :as atoms]
            [reverie.core :as rev]
            [reverie.util :as util])
  (:use reverie.entity
        [reverie.middleware :only [wrap-access]]
        [ring.middleware.json :only [wrap-json-params
                                     wrap-json-response]]))

(defn- init-root-page? []
  (=
   0
   (->
    (k/select page
              (k/aggregate (count :*) :count)
              (k/where {:parent 0 :version 0}))
    first
    :count)))

(defn- get-meta []
  {:init-root-page? (init-root-page?)
   :templates (map util/kw->str (keys @atoms/templates))
   :objects (map util/kw->str (keys @atoms/objects))
   :apps (map util/kw->str (keys @atoms/apps))})

(rev/defpage "/admin/api" {:middleware [[wrap-json-params]
                                        [wrap-json-response]
                                        [wrap-access :edit]]}
  [:get ["/meta"]
   (get-meta)])
