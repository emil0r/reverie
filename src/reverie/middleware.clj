(ns reverie.middleware
  (:require [korma.core :as k]
            [reverie.atoms :as atoms]
            [reverie.auth.user :as user]
            [reverie.responses :as r])
  (:use reverie.entity))

(defn wrap-admin [handler]
  (fn [{:keys [uri] :as request}]
    (if (and
         (re-find #"^/admin" uri)
         (not (re-find #"^/admin/login" uri))
         (not (re-find #"^/admin/logout" uri)))
      (if (or (user/staff?) (user/admin?))
        (handler request)
        (r/response-302 "/admin/login"))
      (handler request))))

(defn wrap-edn-response [handler & [encoding]]
  (fn [request]
    (let [resp (handler request)
          encoding (or "utf-8" encoding)]
      (assoc-in resp [:headers "Content-Type"] (str "application/edn; charset=" encoding "")))))


(defn wrap-edit-mode [handler]
  (fn [{:keys [uri] :as request}]
    (let [u (user/get)
          user-name (:name u)
          request (assoc-in request [:reverie :editor?]
                            (or (user/admin? u)
                                (user/staff? u)))]
      (if (atoms/edit? uri)
        (if (atoms/edit? uri user-name)
          (handler (assoc-in request [:reverie :mode] :edit))
          (handler (assoc-in request [:reverie :mode] :edit-other)))
        (if (atoms/editing? user-name)
          (do
            (atoms/view! user-name)
            (atoms/edit! uri user-name)
            (handler (assoc-in request [:reverie :mode] :edit)))
          (handler (assoc-in request [:reverie :mode] :view)))))))

(defn wrap-published? [handler]
  (fn [{:keys [uri mode] :as request}]
    (let [u (user/get)]
     (if (or (user/admin? u) (user/staff? u))
       (handler request)
       (if-let [[route-uri route-data] (atoms/get-route uri)]
         (if (or (:published? route-data) (= :page (:type route-data)))
           (handler request)
           r/response-404)
         r/response-404)))))
