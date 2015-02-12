(ns reverie.admin.auth
  (:require [hiccup.form :as form]
            [reverie.auth :as auth]
            [reverie.core :refer [defpage]]
            [reverie.response :as response]))



(defn login-view [request page params] {})

(defn handle-login [request {:keys [database] :as page}
                    {:keys [username password]}]
  (if (auth/login database username password)
      (response/get 302 "/admin")
      (response/get 302 "/admin/login")))


(defn logout [request page params]
  (auth/logout)
  (response/get 302 "/"))


(defpage "/admin/login" {:template :admin/login}
  [["/" {:get login-view :post handle-login}]])

(defpage "/admin/logout" {}
  [["/" {:get logout}]])
