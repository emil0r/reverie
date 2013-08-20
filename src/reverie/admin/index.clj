(ns reverie.admin.index
  ;; import defpages
  (:require [reverie.admin.auth :as auth]
            [reverie.admin.frames :as frames]
            ;; import rest
            [reverie.admin.templates :as t]
            [reverie.core :as rev]))


(rev/defpage "/admin" {}
  [:get ["*"] (t/main {:title "Admin"}
                      [:frameset {:id :top :name :top :cols "240px,*"}
                       [:frameset {:id :control :name :control}
                        [:frame {:src "/admin/frame/left" :noresize "noresize" :frameborder "no"}]]
                       [:frameset {:id :main :name :main}
                        [:frame {:src "/" :frameborder "no"}]]])
   ])
