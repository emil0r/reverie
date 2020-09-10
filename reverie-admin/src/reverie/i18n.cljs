(ns reverie.i18n
  (:require [com.stuartsierra.component :as component]
            [re-frame.core :as rf]
            [tongue.core :as tongue]
            [taoensso.timbre :as log]))

(rf/reg-sub :locale/locale (fn [db [_ fallback]]
                             (or (:locale/locale db) fallback)))

(defn init-i18n [data]
  ;; this is a bit hacky
  (def translate (tongue/build-translate data)))


(def locale-sub (rf/subscribe [:locale/locale :en]))

(defn t [& args]
  (apply translate @locale-sub args))

(defn t-with-locale [locale & args]
  (apply translate locale args))


(rf/reg-event-fx :i18n/initialize (fn [_ [_ dictionary]]
                                    (init-i18n dictionary)
                                    nil))

(rf/reg-event-fx :locale/locale (fn [{:keys [db]} [_ locale]]
                                  {:db (assoc-in db [:locale/locale] locale)
                                   :dispatch [:locale/status :changed]}))

(rf/reg-sub :locale/locale (fn [db _]
                             (:locale/locale db)))

(def dicts
  {:en {:locale "Locale"

        :auth/username "Username"
        :auth/password "Password"
        :auth/incorrect-credentials "Incorrect credentials"

        :auth/login "Login"
        :auth/logout "Logout"
        
        :form/cancel "Cancel"
        :form/save "Save"
        :form/add "Add"
        :form/save+continue "Save and continue"
        :form/save+add "Save and add another"}
   :tongue/fallback :en})


(defn- init-i18n! []
  (init-i18n dicts))

(comment
  (init-i18n!)
  )


(defrecord I18nManager [started?]
  component/Lifecycle
  (start [this]
    (if started?
      this
      (do (log/info "Starting I18nManager")
          (init-i18n!)
          (log/info "i18n loaded")
          (assoc this
                 :started? true))))
  (stop [this]
    (if-not started?
      this
      (do (log/info "Stopping I18nManager")
          (assoc this
                 :started? false)))))

(defn i18n-manager [settings]
  (map->I18nManager settings))
