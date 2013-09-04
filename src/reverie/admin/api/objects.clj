(ns reverie.admin.api.objects
  (:require [korma.core :as k]
            [reverie.auth.user :as user]
            [reverie.core :as rev]
            [reverie.atoms :as atoms]
            [reverie.page :as page])
  (:use reverie.entity
        [reverie.util :only [published?]]
        [ring.middleware.json :only [wrap-json-params
                                     wrap-json-response]]))


(rev/defpage "/admin/api/objects" {:middleware [[wrap-json-params]
                                                [wrap-json-response]]}
  [:get ["/add/:page-serial/:area/:object"]
   {:result true}])
