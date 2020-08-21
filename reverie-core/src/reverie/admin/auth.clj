(ns reverie.admin.auth
  (:require [muuntaja.middleware :refer [wrap-format]]
            [reverie.auth :as auth]
            [reverie.core :refer [defpage]]
            [reverie.http.middleware :refer [wrap-authn
                                             wrap-authz]]
            [reverie.http.negotiation :refer [muuntaja-instance]]
            [reverie.http.response :as response]
            [reverie.session :as session]))



(defn auth-get [request page params]
  {:status 200
   :body (get-in request [:reverie :user])})

(defn auth-post [{{database :database} :reverie body-params :body-params :as request} page params]
  (if-let [user (auth/login body-params database)]
    (do
      (session/swap! request merge {:user-id (:id user)})
      {:status 200
       :body user})
    {:status 400
     :body "Unable to login"}))


(defn logout [request page params]
  (auth/logout request)
  {:status 200
   :body {:result :success}})


(defpage "/admin/auth"
  {:middleware [[wrap-format muuntaja-instance]]}
  [["/" {:get auth-get :post auth-post}]])

(defpage "/admin/auth/logout"
  {}
  [["/" {:post logout}]])
