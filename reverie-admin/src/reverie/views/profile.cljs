(ns reverie.views.profile
  (:require ["antd" :refer [Button]]
            ["@ant-design/icons" :refer [UserOutlined]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as rfe]
            [reverie.i18n :refer [t]]))


(defn sidebar-details []
  (let [profile (rf/subscribe [:auth/profile])]
    (fn []
      [:div.profile
       {:on-click #(rfe/push-state :profile/index)}
       [:> UserOutlined]
       (:username @profile)
       [:div
        [:> Button
         {:on-click #(rf/dispatch [:auth/logout])}
         (t :auth/logout)]]])))


(defn index [context]
  [:div "Profile"])
