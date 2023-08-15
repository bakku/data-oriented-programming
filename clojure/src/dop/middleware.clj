(ns dop.middleware
  (:require [clojure.string :as string]
            [ring.util.response :refer [status]])
  (:import (java.nio.charset StandardCharsets)
           (java.util Base64)))

(defn- get-auth-header-value
  [request]
  (get-in request [:headers "authorization"]))

(defn- decode-base64
  [^String encoded]
  (try
    (-> (Base64/getDecoder)
        (.decode encoded)
        (String. StandardCharsets/UTF_8))
    (catch Exception _ nil)))

(defn- decode-basic-auth-value
  [auth-value]
  (-> auth-value
      ;; Remove "Basic " part
      (subs 6)
      (decode-base64)))

(defn- try-authenticate
  [authenticate auth-value]
  (if-let [decoded-auth-value (decode-basic-auth-value auth-value)]
    (let [[username password] (string/split decoded-auth-value #":")]
      (if (and (not (nil? username))
               (not (nil? password)))
        (authenticate username password)
        false))
    false))

(defn basic-auth-middleware
  [handler authenticate]
  (fn [request]
    (if-let [auth-value (get-auth-header-value request)]
      (if-let [user (try-authenticate authenticate auth-value)]
        (handler (assoc request :user user))
        (status 401))
      (status 401))))

(defn- decode-bearer-auth-value
  [auth-value]
  (if (<= 7 (count auth-value))
    ;; Remove "Bearer " part
    (subs auth-value 7)
    nil))

(defn- try-authenticate-with-token
  [authenticate auth-value]
  (if-let [token (decode-bearer-auth-value auth-value)]
    (authenticate token)
    false))

(defn bearer-middleware
  [handler authenticate & predicates]
  (fn [request]
    (if-let [auth-value (get-auth-header-value request)]
      (if-let [user (try-authenticate-with-token authenticate auth-value)]
        (if (every? #(% user) predicates)
          (handler (assoc request :user user))
          (status 403))
        (status 401))
      (status 401))))