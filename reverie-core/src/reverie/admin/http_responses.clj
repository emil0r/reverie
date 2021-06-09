(ns reverie.admin.http-responses)

(def failure {:status :failure})
(def success {:status :success})
(def no-page-rights {:status :failure
                     :error :reverie.page/no-edit-rights})
