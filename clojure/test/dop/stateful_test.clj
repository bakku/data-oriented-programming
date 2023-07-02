(ns dop.stateful-test
  (:require [clojure.test :refer :all]
            [malli.generator :as mg]
            [dop.schema :as schema]
            [dop.stateful :refer :all]))

(deftest get-restaurants-test
  (reset! app-state {:restaurants
                     (->> (repeatedly #(mg/generate schema/restaurant))
                          (take 2)
                          vec)})
  (testing "returns all restaurants from the app state"
    (swap! app-state
           (fn [state]
             (assoc-in state
                       [:restaurants 0 :name]
                       "The Blue Dolphin")))
    (is (= 2 (count (get-restaurants))))
    (is (= "The Blue Dolphin" (:name (first (get-restaurants)))))))

(deftest create-restaurant-test
  (reset! app-state {:restaurants
                     (->> (repeatedly #(mg/generate schema/restaurant))
                          (take 2)
                          vec)})
  (testing "adds a new restaurant to the app state"
    (create-restaurant {:name "The Blue Dolphin"})
    (is (= 3 (count (:restaurants @app-state))))
    (is (= "The Blue Dolphin" (get-in @app-state [:restaurants 2 :name])))))
