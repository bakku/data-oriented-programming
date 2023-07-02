(ns dop.stateful
  (:require [dop.schema :as schema]))

(def app-state
  (atom {:restaurants [{:name "The Blue Dolphin"}
                       {:name "The Red Lion"}]}))

(defn get-restaurants
  {:malli/schema [:=> :cat [:vector schema/restaurant]]}
  []
  (@app-state :restaurants))

(defn create-restaurant
  {:malli/schema [:=> [:cat schema/restaurant] :nil]}
  [restaurant]
  (swap! app-state
         (fn [state]
           (update-in state
                      [:restaurants]
                      #(conj % restaurant))))
  nil)
