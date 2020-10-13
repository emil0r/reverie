(ns reverie.views
  (:require ["antd" :refer [Layout Layout.Header Layout.Sider Layout.Content
                            Menu Menu.Item
                            Tabs Tabs.TabPane]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as rfe]
            [reagent.core :as r]
            [reverie.routes :refer [routes router]]
            [reverie.views.auth :as views.auth]
            [reverie.views.profile :as views.profile]
            [reverie.views.root]
            [reverie.views.sidebar :as views.sidebar]))

(defonce matched-data (r/atom nil))

(defn content []
  (if-let [m @matched-data]
    (let [view (-> m :data :view)]
      [view m])
    [reverie.views.root/index nil]))

(defn init! []
  (rfe/start!
   router
   (fn [m] (reset! matched-data m))
   {:use-fragment true}))

(defn menu []
  [:> Menu
   {:theme "light"
    :mode "horizontal"}
   [:> Menu.Item
    [views.auth/logout]]])

(defn index []
  (let [logged-in? (rf/subscribe [:auth/logged-in?])]
    (fn []
      (init!)
      (if-not @logged-in?
        [views.auth/login]
        [:> Layout
         {:theme "light"
          :style {:min-height "100vh"}}
         [:> Layout
          [:> Layout.Sider
           {:theme "light"
            :class "reverie-sidebar"}
           [:img.logo {:src "/img/reveriecms-logo.png"
                       :on-click #(rfe/push-state :root/index)}]
           [views.sidebar/index]
           [views.profile/sidebar-details]]
          [:> Layout.Content
           [content]]]]))))
