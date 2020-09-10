(ns reverie.views
  (:require ["antd" :refer [Layout Layout.Header Layout.Sider Layout.Content]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as rfe]
            [reagent.core :as r]
            [reverie.routes :refer [routes router]]
            [reverie.views.auth :as views.auth]
            [reverie.views.root]))

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
  [:div "Menu"])

(defn index []
  (let [logged-in? (rf/subscribe [:user/logged-in?])]
    (fn []
      (init!)
      (if-not @logged-in?
        [views.auth/login]
        [:> Layout
         {:theme "light"
          :style {:min-height "100vh"}}
         [:> Layout.Header
          [menu]]
         [:> Layout
          [:> Layout.Sider
           {:theme "light"}
           "Sider"]
          [:> Layout.Content
           [content]]]]
        #_[:> ant/Layout
         {:theme "light"
          :style {:minHeight "100vh"}}
         [:> ant/Layout.Header
          {:theme "light"}
          [menu]]
         [:> ant/Layout
          [:> ant/Layout.Content
           {:style {:padding "20px"}}
           [breadcrumbs router (-> @matched-data :path)]
           [:div.modules
            [content]]]]]))))
