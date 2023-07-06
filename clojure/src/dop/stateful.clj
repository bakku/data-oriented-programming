(ns dop.stateful
  (:require [crypto.password.bcrypt :as bcrypt]
            [com.rpl.specter :as specter]
            [dop.schema :as schema]))

(def app-state
  (atom {:restaurants [{:name "The Blue Dolphin"}
                       {:name "The Red Lion"}]
         :users [{:username "bakku"
                  :password (bcrypt/encrypt "test1234")
                  :admin true}]}))

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

(defn- generate-token
  []
  (->> #(rand-nth "abcdefghijklmnopqrstuvwxyz0123456789_-")
       repeatedly
       (take 30)
       (apply str)))

(defn- add-token-for-user
  [state user token]
  (specter/transform [:users specter/ALL #(= (% :username) (user :username))]
                     #(assoc % :access-token token)
                     state))

(defn generate-and-store-token
  {:malli/schema [:=> [:cat schema/user] string?]}
  [user]
  (let [token (generate-token)]
    (swap! app-state #(add-token-for-user % user token))
    token))

(defn authenticate
  [username password]
  (if-let [user (->> (@app-state :users)
                     (filter #(= (% :username) username))
                     first)]
    (if (bcrypt/check password (user :password))
      user
      nil)))
