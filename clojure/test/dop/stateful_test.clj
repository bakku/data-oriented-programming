(ns dop.stateful-test
  (:require [clojure.test :refer :all]
            [crypto.password.bcrypt :as bcrypt]
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

(deftest generate-and-store-token-test
  (reset! app-state {:users [(mg/generate schema/user)]})
  (with-redefs [dop.stateful/generate-token (fn [] "token")]
    (testing "updates token in app-state"
      (generate-and-store-token (first (@app-state :users)))
      (is (= "token"
             (-> (@app-state :users)
                 first
                 (get :access-token)))))
    (testing "returns the token"
      (is (= "token"
             (generate-and-store-token (first (@app-state :users))))))))

(deftest authenticate-test
  (reset! app-state {:users [(assoc (mg/generate schema/user) :password (bcrypt/encrypt "test")
                                                              :username "testuser")]})
  (testing "returns nil if user does not exist"
    (is (nil? (authenticate "test" "test"))))
  (testing "returns nil if password does not match"
    (is (nil? (authenticate "testuser" "bla"))))
  (testing "returns user if username and password matches"
    (is (= (first (@app-state :users))
           (authenticate "testuser" "test")))))

(deftest authenticate-via-token-test
  (reset! app-state {:users [(assoc (mg/generate schema/user) :access-token "12345")]})
  (testing "returns nil if no user cannot be found for token"
    (is (nil? (authenticate-via-token "54321"))))
  (testing "returns user if user can be found for token"
    (is (= (first (@app-state :users))
           (authenticate-via-token "12345")))))