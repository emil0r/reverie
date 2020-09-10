(ns reverie.views.auth
  (:require ["antd" :refer [Button Input Input.Password]]
            [ez-wire.form :as form]
            [ez-wire.form.helpers :refer [valid? add-external-error]]
            [re-frame.core :as rf]
            [reverie.communication :refer [success? POST]]
            [reverie.form.adapter :refer [text-adapter]]
            [reverie.i18n :refer [t]])
  (:require-macros [ez-wire.form.macros :refer [defform]]
                   [ez-wire.form.validation :refer [defvalidation]]))

(defn logout []
  [:a
   {:href "#"
    :on-click #(rf/dispatch [:auth/logout])}
   (t :auth/logout)])

(defn- handle-faulty-login-response [form]
  (fn [data]
    (add-external-error form :password :unauthorized (t :auth/incorrect-credentials) true)))

(defn- login! [form username password]
  (POST "/admin/auth"
        {:username username
         :password password}
        (fn [data]
          (if (success? data)
            (rf/dispatch [:user/profile (:payload data)])
            (add-external-error form :password :response (t (:payload data)) true)))
        (handle-faulty-login-response form)))

(defform loginform
  {}
  [{:element Input
    :adapter text-adapter
    :placeholder :auth/username
    :label (t :auth/username)
    :name :username}
   {:element Input.Password
    :adapter text-adapter
    :placeholder :auth/password
    :label (t :auth/password)
    :name :password}])

(defn login []
  (let [form (loginform {:username ""
                         :password ""})
        valid-form (rf/subscribe [::form/on-valid (:id form)])]
    (fn []
      [:div.login-splash
       [:form.login
        [:div
         [:img {:src "/img/reveriecms.png"}]
         [form/as-table {} form]
         [:> Button {:type "primary"
                     :disabled (not (valid? valid-form))
                     :on-click #(when (valid? valid-form)
                                  (let [data @valid-form]
                                    (login! form (:username data) (:password data))))}
          (t :auth/login)]]]])))

