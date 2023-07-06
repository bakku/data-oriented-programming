(ns dop.schema)

(def user
  [:map
   [:username string?]
   [:password string?]
   [:access-token {:optional true} string?]
   [:admin boolean?]])

(def restaurant
  [:map
   [:name string?]])