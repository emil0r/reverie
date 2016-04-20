(ns reverie.admin.api.editors
  (:require [clj-time.core :as t]
            [clojure.core.match :refer [match]]
            [clojure.string :as str]
            [reverie.internal :refer [read-storage write-storage delete-storage]]
            [reverie.page :as page]
            [reverie.scheduler :as scheduler])
  (:import [reverie.page Page AppPage RawPage]))

(defonce edits (atom {}))
(defonce editors (atom {}))

(defn get-edit-by-user [user]
  (when user
    (->> @edits
         (filter (fn [[_ {:keys [user-id]}]]
                   (= (:id user) user-id)))
         first
         second)))

(defn edit! [page user]
  (let [edit (get-edit-by-user user)]
    (match [(cond
             (nil? edit) :free
             :else :editing)

            (cond
             (and
              (not (nil? edit))
              (= edit (get @edits (page/serial page)))) :same
             (not (nil? (get @edits (page/serial page)))) :other
             :else :free)]
           [_ :other] {:success false :error "Someone else is editing this page"}
           [_ :same] true
           [:editing _] (do (swap! edits dissoc (:serial edit))
                            (swap! edits assoc (page/serial page)
                                   {:user-id (:id user)
                                    :serial (page/serial page)
                                    :time (t/now)})
                            true)
           [:free _] (do (swap! edits assoc (page/serial page)
                                {:user-id (:id user)
                                 :serial (page/serial page)
                                 :time (t/now)})
                         true))))

(defn stop-edit! [user]
  (doseq [[k {:keys [user-id]}] @edits]
    (if (= user-id (:id user))
      (swap! edits dissoc k)))
  true)

(defn- page? [page]
  (condp = (type page)
    Page true
    AppPage true
    RawPage true
    false))

(defn edit? [page user]
  (and (page? page)
       (not (nil? user))
       (not (nil? page))
       (= (:id user) (:user-id (get @edits (page/serial page))))))


(defn editor! [user]
  {:pre [(not (nil? (:id user)))]}
  (swap! editors assoc (:id user) (t/now)))


(defn editor? [user]
  (and (not (nil? (:id user)))
       (contains? @editors (:id user))))


(defn edit-follow! [page user]
  (when-not (= :module (page/type page))
    (when-let [edit (get-edit-by-user user)]
      (swap! edits dissoc (:serial edit))
      (swap! edits assoc (page/serial page)
             {:user-id (:id user)
              :serial (page/serial page)
              :time (t/now)}))))

(defn assoc-admin-links [page request response]
  (if (get-in request [:reverie :edit?])
    (assoc response :body
           (-> (:body response)
               (str/replace
                #"</head>"
                (str "<link rel='stylesheet' href='/static/admin/css/editing.css' type='text/css' />"
                     "</head>"))
               (str/replace
                #"</body>"
                (if (some #(= (page/type page) %) [:page :app])
                  (str "<script type='text/javascript'>parent.dom.\\$m_on_loaded();</script>"
                       "</body>")
                  "</body>"))))
    response))


(defn edits-task-handler! [t {:keys [minutes] :as opts}]
  (let [now (t/now)
        expired-edits (map first
                           (filter (fn [[k {:keys [time]}]]
                                     (t/after? now (t/plus time
                                                           (t/seconds minutes))))
                                   @edits))]
    (when-not (empty? expired-edits)
      (apply swap! edits dissoc expired-edits))))

(defn get-edits-task
  "Scheduled task to run for the editors atom. Minutes determines how many minutes need to pass before a user is removed"
  [minutes]
  (scheduler/get-task {:id :admin-edits
                       :desc "Remove unused edits"
                       :handler edits-task-handler!
                       :schedule "0 * * * * * *" ;; every minute
                       :opts {:minutes minutes}}))
