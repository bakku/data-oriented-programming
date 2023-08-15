(ns dop.core-test
  (:require [clojure.test :refer :all]
            [dop.stateful :refer [app-state]]
            [dop.core :refer :all]))

(deftest app-test
  (reset! app-state {:users [{:username     "testuser"
                              :password     (crypto.password.bcrypt/encrypt "test1234")
                              :access-token "12345"
                              :admin        false}
                             {:username     "testadmin"
                              :password     (crypto.password.bcrypt/encrypt "admin1234")
                              :access-token "54321"
                              :admin        true}]})
  (testing "creating restaurants requires an authenticated admin user"
    (is (= 403
           (get (app {:request-method :post
                      :uri            "/stateful/restaurants"
                      :body           "{\"name\":\"Ganesha Tandoori\"}"
                      :headers        {"content-type"  "application/json"
                                       "authorization" "Bearer 12345"}})
                :status)))
    (is (= 201
           (get (app {:request-method :post
                      :uri            "/stateful/restaurants"
                      :body           "{\"name\":\"Ganesha Tandoori\"}"
                      :headers        {"content-type"  "application/json"
                                       "authorization" "Bearer 54321"}})
                :status)))))