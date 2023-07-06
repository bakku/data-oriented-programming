(ns dop.middleware-test
  (:require [clojure.test :refer :all]
            [reitit.ring :as ring]
            [crypto.password.bcrypt :as bcrypt]
            [dop.middleware :refer :all]))

(deftest basic-auth-middleware-test
  (let [users [{:username "testuser"
                :password (bcrypt/encrypt "test1234")
                :admin false}]
        authenticate (fn [username password]
                    (if-let [user (first (filter #(= (% :username) username) users))]
                      (if (bcrypt/check password (user :password))
                        user
                        nil)))
        test-app (ring/ring-handler
                   (ring/router
                     [["/test" {:get        (fn [request] {:status 200 :body "OK" :user (request :user)})
                                :middleware [[basic-auth-middleware authenticate]]}]]))]
    (testing "returns unauthorized if no header is passed at all"
      (is (= {:status 401
              :body nil
              :headers {}}
             (test-app {:request-method :get :uri "/test"}))))
    (testing "returns unauthorized if no authorization header is passed"
      (is (= {:status 401
              :body nil
              :headers {}}
             (test-app {:request-method :get
                        :uri "/test"
                        :headers {:some-header "Some-Value"}})))
      (is (= {:status 401
              :body nil
              :headers {}}
             (test-app {:request-method :get
                        :uri "/test"
                        ;; dGVzdHVzZXI= => testuser
                        :headers {"authorization" "Basic dGVzdHVzZXI="}}))))
    (testing "returns unauthorized if authenticate fails"
      (is (= {:status 401
              :body nil
              :headers {}}
             (test-app {:request-method :get
                        :uri "/test"
                        ;; dGVzdHVzZXI6dGVzdDEyMzQ= => testuse:test1234
                        :headers {"authorization" "Basic dGVzdHVzZTp0ZXN0MTIzNA=="}}))))
    (testing "returns ok if authenticate passes"
      (is (= {:status 200
              :body "OK"
              :user (first users)}
             (test-app {:request-method :get
                        :uri "/test"
                        ;; dGVzdHVzZXI6dGVzdDEyMzQ= => testuser:test1234
                        :headers {"authorization" "Basic dGVzdHVzZXI6dGVzdDEyMzQ="}}))))))
