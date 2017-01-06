(ns reverie.admin.frames.controlpanel
  (:require [hiccup.form :as form]
            [reverie.module :as m]
            [reverie.system :as sys]))


(defn controlpanel [request module params]
  (let [{:keys [user dev?]} (:reverie request)]
    {:panel (list
             [:div.user-info "Logged in as " [:span (:full-name user)]
              [:div.logout [:a {:href "#"} "Logout" [:i.fa.fa-off]]]]

             [:div#tabbar
              [:div.goog-tab.goog-tab-selected
               {:tab :navigation-meta} "Navigation"]
              [:div.goog-tab {:tab :modules} "Modules"]]

             [:div.modules.hidden
              [:ul
               (map (fn [{:keys [module] :as mod}]
                      [:li {:module (m/slug module)}
                       (m/name module)])
                    (sort-by
                     #(-> % :module m/name)
                     (-> @sys/storage :modules vals)))]]

             [:div.navigation-meta
              [:div.tree
               [:form#tree-search-form
                (form/text-field :tree-search 1)
                [:i#tree-search-icon.fa.fa-search]]
               [:div#tree]
               [:div.icons
                [:i.fa.fa-refresh {:title "Refresh"}]
                [:i.fa.fa-plus-circle {:title "Add page"}]
                [:i.fa.fa-pencil {:title "Edit mode"}]
                [:i.fa.fa-eye.hidden {:title "View mode"}]
                [:i.fa.fa-trash {:title "Trash it"}]]]
              [:div.meta
               [:table.meta
                [:tr [:th "Name"] [:td.name ""]]
                [:tr [:th "Title"] [:td.title ""]]
                [:tr [:th "Created"] [:td.created ""]]
                [:tr [:th "Updated"] [:td.updated ""]]
                [:tr [:th "Published?"] [:td.published_p ""]]]
               [:div.buttons
                [:div.btn.btn-primary.publish "Publishing"]
                [:div.btn.btn-primary.meta "Meta"]]]])}))
