(ns reverie.test.scheduler
  (:require [com.stuartsierra.component :as component]
            [cronj.core :as cronj]
            [reverie.scheduler :as scheduler]
            [midje.sweet :refer :all]))

(defn inc-handler [t {:keys [storage]}]
  (swap! storage inc))


(fact
 "basic scheduling"
 (let [storage (atom 0)
       t (scheduler/get-task {:id :test
                              :handler inc-handler
                              :schedule "* * * * * * *"
                              :opts {:storage storage}})
       s (component/start (scheduler/get-scheduler [t]))]
   (scheduler/start! s)
   (Thread/sleep 5000)
   (component/stop s)
   (zero? @storage) => false))


(fact
 "scheduling - add tasks after creation"
 (let [s1 (atom 0)
       s2 (atom 0)
       s3 (atom 0)
       s4 (atom 0)
       t1 (scheduler/get-task {:id :test1
                               :handler inc-handler
                               :schedule "* * * * * * *"
                               :opts {:storage s1}})
       t2 (scheduler/get-task {:id :test2
                               :handler inc-handler
                               :schedule "* * * * * * *"
                               :opts {:storage s2}})
       t3 (scheduler/get-task {:id :test3
                               :handler inc-handler
                               :schedule "* * * * * * *"
                               :opts {:storage s3}})
       t4 (scheduler/get-task {:id :test4
                               :handler inc-handler
                               :schedule "* * * * * * *"
                               :opts {:storage s4}})
       s (component/start (scheduler/get-scheduler [t1]))]
   (scheduler/add-task! s t2)
   (scheduler/add-tasks! s [t3 t4])
   (scheduler/start! s)
   (Thread/sleep 5000)
   (component/stop s)
   (map #(-> % deref zero?) [s1 s2 s3 s4]) => [false false false false]))
