(ns reverie.admin.options.object
  "This namespace is only run in the frame for object edits"
  (:require [clojure.string :as s]
            [goog.events :as events]
            [goog.ui.TabBar :as tb]
            [goog.ui.Tab :as t]
            [goog.dom :as dom]
            [jayq.core :as jq]
            [jayq.util :as util])
  (:use [reverie.util :only [ev$ query-params params->querystring join-uri]]))


(defn click-richtext! [e]
  (let [params (query-params (-> js/window .-location .-href))
        field (-> e ev$ (jq/attr :field-name))]
    (.open js/window
           (str
            "/admin/frame/object/edit/richtext?form=form_object&field="
            field
            "&"
            (params->querystring params))
           "_blank"
           "height=640,width=800,location=0,menubar=0,resizable=1,scrollbars=1,status=0,titlebar=1"
           true)))

(defn click-image! [e]
  (let [field (-> e ev$ (jq/attr :field-name))
        value (-> (str "#" field) jq/$ jq/val)
        url (apply join-uri "/admin/frame/file-picker/images"
                   (butlast (rest (remove s/blank? (s/split value #"/")))))]
    (.open js/window
           (str
            url "?form=form_object&field-name=" field "&value=" value)
           "_blank"
           "height=640,width=800,location=0,menubar=0,resizable=1,scrollbars=1,status=0,titlebar=1"
           true)))

(defn click-url! [e]
  (let [field (-> e ev$ (jq/attr :field-name))
        value (-> (str "#" field) jq/$ jq/val)
        url "/admin/frame/url-picker"]
    (.open js/window
           (str
            url "?form=form_object&field-name=" field "&value=" value)
           "_blank"
           "height=640,width=800,location=0,menubar=0,resizable=1,scrollbars=1,status=0,titlebar=1"
           true)))

(defn init []
  (-> js/document
      jq/$
      (jq/off "span[type=richtext]")
      (jq/off "span[type=image]")
      (jq/off "span[type=url]"))
  (-> js/document
      jq/$
      (jq/on :click "span[type=richtext]" click-richtext!)
      (jq/on :click "span[type=image]" click-image!)
      (jq/on :click "span[type=url]" click-url!))
  (doseq [input (jq/$ "[_type=datetime]")]
    (.appendDtpicker (jq/$ input) (clj->js {:minuteInterval 15
                                            :firstDayOfWeek 1
                                            :current ""
                                            :autodateOnStart false})))
  (doseq [input (jq/$ "[_type=date]")]
    (.appendDtpicker (jq/$ input) (clj->js {:minuteInterval 15
                                            :firstDayOfWeek 1
                                            :current ""
                                            :dateOnly true
                                            :autodateOnStart false}))))
