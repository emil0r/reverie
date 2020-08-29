(ns reverie.views
  (:require ["antd" :refer [Button]]))

(defn index []
  [:div.login-splash
   [:img {:src "/img/reveriecms.png"}]
   [:div.username [:input]]
   [:div.password [:input {:type :password}]]
   [:div
    [:> Button {:type "primary"}
     "Login"]]])
