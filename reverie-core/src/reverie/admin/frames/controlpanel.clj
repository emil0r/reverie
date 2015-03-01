(ns reverie.admin.frames.controlpanel
  (:require [hiccup.form :as form]
            [reverie.module :as m]
            [reverie.system :as sys]))


(defn controlpanel [request module params]
  (let [{:keys [user dev?]} (:reverie request)]
    {:panel (list
             [:div.user-info "Logged in as " [:span (:full-name user)]
              [:div.logout [:a {:href "#"} "Logout" [:i.icon-off]]]]

             [:div#tabbar
              [:div.goog-tab.goog-tab-selected
               {:tab :navigation-meta} "Navigation"]
              [:div.goog-tab {:tab :modules} "Modules"]]

             [:div.modules.hidden
              [:ul
               (map (fn [{:keys [module] :as mod}]
                      [:li {:module (m/slug module)}
                       (m/name module)])
                    (-> @sys/storage :modules vals))]]

             [:div.navigation-meta
              [:div.tree
               [:form#tree-search-form
                (form/text-field :tree-search 1)
                [:i#tree-search-icon.icon-search]]
               [:div#tree]
               [:div.icons
                [:i.icon-refresh {:title "Refresh"}]
                [:i.icon-plus-sign {:title "Add page"}]
                [:i.icon-edit-sign {:title "Edit mode"}]
                [:i.icon-eye-open.hidden {:title "View mode"}]
                [:i.icon-trash {:title "Trash it"}]]]
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
