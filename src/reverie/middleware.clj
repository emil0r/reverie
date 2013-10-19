(ns reverie.middleware
  (:require [clojure.string :as s]
            [reverie.atoms :as atoms]
            [reverie.auth.user :as user]
            [reverie.page :as page]
            [reverie.response :as r])
  (:use reverie.entity))

(defn wrap-admin [handler]
  (fn [{:keys [uri] :as request}]
    (let [u (user/get)]
     (if (and
          (re-find #"^/admin" uri)
          (not (re-find #"^/admin/login" uri))
          (not (re-find #"^/admin/logout" uri)))
       (if (or (user/admin? u) (user/staff? u))
         (handler request)
         (r/response-302 "/admin/login"))
       (handler request)))))

(defn wrap-edn-response [handler & [encoding]]
  (fn [request]
    (let [resp (handler request)
          encoding (or "utf-8" encoding)]
      (assoc-in resp [:headers "Content-Type"] (str "application/edn; charset=" encoding "")))))


(defn wrap-edit-mode [handler]
  (fn [{:keys [uri] :as request}]
    (let [u (user/get)
          user-name (:name u)]
      (if (atoms/edit? uri)
        (if (and
             user-name
             uri
             (atoms/edit? uri user-name))
          (handler (assoc-in request [:reverie :mode] :edit))
          (handler (assoc-in request [:reverie :mode] :edit-other)))
        (if (and
             user-name
             (atoms/editing? user-name)
             (not (re-find #"^/admin" uri)))
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
           (r/response-404))
         (r/response-404))))))

(defn wrap-access [handler roles & [response]]
  (fn [request]
    (let [u (user/get)]
      (if (or (user/admin?)
              (user/role? u roles))
        (handler request)
        (r/response-401 response)))))

(defn wrap-reverie-data
  "Add interesting reverie data into the request map for functions further down the stream. Handle 404's here as well"
  [handler]
  (fn [{:keys [uri] :as request}]
    (if-let [[_ route-data] (atoms/get-route uri)]
      (let [u (user/get)
            editor? (or (user/admin? u)
                        (user/role? u :edit))
            p (page/get {:serial (:serial route-data)
                         :version (if editor? 0 1)})]
        (handler (-> request
                     (assoc-in [:reverie :editor?] editor?)
                     (assoc-in [:reverie :route-data] route-data)
                     (assoc-in [:reverie :page] p)
                     (assoc-in [:reverie :page-id] (:id p))
                     (assoc-in [:reverie :page-serial] (:serial p)))))
      r/response-404)))

(defn wrap-redirects
  "Take care of redirects"
  [handler]
  (fn [{:keys [uri] :as request}]
    (if-let [[_ {:keys [serial]}] (atoms/get-route uri)]
      (let [{p :page editor? :editor?} (:reverie request)
            {:keys [redirect redirect-permanent]} (:attributes p)
            redirect (s/trim (:value redirect))
            redirect-permanent (s/trim (:value redirect-permanent))]
        (cond
         (re-find #"^\d+$" redirect) (let [p (page/get {:serial (read-string redirect)
                                                        :version (if editor? 0 1)})]
                                       (r/response-302 (:uri p)))
         (not (s/blank? redirect)) (r/response-302 redirect)
         (re-find #"^\d+$" redirect-permanent) (let [p (page/get {:serial (read-string redirect-permanent)
                                                                  :version (if editor? 0 1)})]
                                                 (r/response-301 (:uri p)))
         (not (s/blank? redirect-permanent)) (r/response-301 redirect-permanent)
         :else (handler request)))
      (handler request))))
