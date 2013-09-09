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


(rev/defpage "/admin/api/images" {:middleware [[wrap-json-params]
                                               [wrap-json-response]
                                               [wrap-access :edit]]}
  [:get ["/list"]
   [{:title "image 1" :value "https://lh5.googleusercontent.com/-KEKo4D2u3c4/AAAAAAAAAAI/AAAAAAAAEK4/Y_AW9RoYQZc/s46-c-k-no/photo.jpg"}
    {::title "image 2" :value "https://0.gravatar.com/avatar/7c4f8620ee823319b0b7227ad0f36133?d=https%3A%2F%2Fidenticons.github.com%2Fb4bf04d0f6c635d8b2d79ea06b4d7feb.png&s=140"}]])
