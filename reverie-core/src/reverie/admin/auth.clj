(ns reverie.admin.auth
  (:require [hiccup.form :as form]
            [reverie.admin.api.editors :as editors]
            [reverie.auth :as auth]
            [reverie.core :refer [defpage]]
            [reverie.http.response :as response]
            [reverie.session :as session]))



(defn login-view [request page params] {})

(defn handle-login [request {:keys [database] :as page}
                    params]
  (if-let [user (auth/login params database)]
    (do
      (session/swap! request merge {:user-id (:id user)})
      (editors/editor! user)
      (response/get 302 "/admin"))
    (response/get 302 "/admin/login")))


(defn logout [request page params]
  (auth/logout request)
  (response/get 302 "/"))


(defpage "/admin/login" {:template :admin/login
                         :forgery? true}
  [["/" {:get login-view :post handle-login}]])

(defpage "/admin/logout" {}
  [["/" {:get logout}]])
