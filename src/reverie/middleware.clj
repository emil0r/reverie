(ns reverie.middleware
  (:require [korma.core :as k]
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
