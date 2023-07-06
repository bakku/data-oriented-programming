(ns dop.core
  (:require [reitit.ring :as ring]
            [reitit.ring.coercion :as coercion]
            [reitit.coercion.malli :as coercion-malli]
            [reitit.ring.middleware.muuntaja :as reitit-muuntaja]
            [reitit.openapi :as openapi]
            [reitit.swagger-ui :as swagger-ui]
            [org.httpkit.server :as hk-server]
            [muuntaja.core :as muuntaja]
            [ring.util.response :refer [response created]]
            [malli.dev :as dev]
            [dop.stateful :as stateful]
            [dop.schema :as schema]
            [dop.middleware :as middleware])
  (:gen-class))


;; reitit.swagger-ui does not use Swagger UI 5 yet,
;; so we have to act as if an openapi 3.0.0 documentation
;; is generated.
(def openapi-route
  {:no-doc true
   :openapi {:openapi "3.0.0"
             :info {:title "Data Oriented Programming with Clojure"
                    :version "0.0.1"}
             :components {:securitySchemes {"basic" {:type :http
                                                     :scheme :basic}}}}
   :handler (openapi/create-openapi-handler)})

(defn status-handler
  [_]
  (response {:status "I'm up!"}))

(def status-route
  {:responses {200 {:body [:map
                           [:status string?]]}}
   :handler   status-handler})

(defn stateful-get-restaurants-handler
  [_]
  (response (stateful/get-restaurants)))

(def stateful-get-restaurants-route
  {:responses {200 {:body [:vector schema/restaurant]}}
   :handler   stateful-get-restaurants-handler})

(defn stateful-post-restaurants-handler
  [{restaurant :body-params}]
  (stateful/create-restaurant restaurant)
  (created "/stateful/restaurants"))

(def stateful-post-restaurants-route
  {:parameters {:body schema/restaurant}
   :responses {201 {}}
   :handler stateful-post-restaurants-handler})

(defn stateful-get-auth-token-handler
  [{:keys [user]}]
  (response {:access-token
             (stateful/generate-and-store-token user)}))

(def stateful-get-auth-token-route
  {:parameters {}
   :responses  {200 {:body [:map [:access-token string?]]}}
   :handler    stateful-get-auth-token-handler})

(def app
  (ring/ring-handler
    (ring/router
      [["/openapi.json" {:get openapi-route}]
       ["/status" {:get status-route}]
       ["/stateful"
        ["/restaurants" {:get  stateful-get-restaurants-route
                         :post stateful-post-restaurants-route}]
        ["/auth" {:openapi {:security [{"basic" []}]}}
         ["/token" {:get stateful-get-auth-token-route
                    :middleware [[middleware/basic-auth-middleware stateful/authenticate]]}]]]]
      {:data {:coercion coercion-malli/coercion
              :muuntaja muuntaja/instance
              :middleware [reitit-muuntaja/format-negotiate-middleware
                           reitit-muuntaja/format-response-middleware
                           reitit-muuntaja/format-request-middleware
                           coercion/coerce-exceptions-middleware
                           coercion/coerce-request-middleware
                           coercion/coerce-response-middleware]}})
    (ring/routes
      (swagger-ui/create-swagger-ui-handler
        {:path "/swagger-ui"
         :url "/openapi.json"})
      (ring/create-default-handler))))

(defn -main
  []
  (dev/start!)
  (hk-server/run-server app {:port 3000}))