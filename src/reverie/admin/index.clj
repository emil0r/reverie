(ns reverie.admin.index
  ;; import defpages
  (:require reverie.admin.api
            reverie.admin.api.objects
            reverie.admin.api.pages
            reverie.admin.auth
            reverie.admin.frames
            reverie.admin.modules
            ;; import rest
            [reverie.admin.templates :as t])
  (:use [reverie.core :only [defpage]]))


(defpage "/admin" {}
  [:get ["/"] (t/main {:title "Admin"}
                      [:frameset {:id :top :name :top :cols "240px,*"}
                       [:frameset {:id :control :name :control}
                        [:frame {:src "/admin/frame/left" :noresize "noresize" :frameborder "no" :id :framec :name :framec}]]
                       [:frameset {:id :main :name :main :cols "*,0"}
                        [:frame {:src "/" :frameborder "no" :id :framem :name :framem}]
                        [:frame {:src "/admin/frame/options" :frameborder "no" :id :frameo :name :frameo}]]])
   ])
