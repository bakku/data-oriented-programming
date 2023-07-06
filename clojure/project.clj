(defproject dop "0.1.0-SNAPSHOT"
  :description "Exhibits all 4 DOP principles using clojure"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [metosin/reitit "0.7.0-alpha5"]
                 [http-kit "2.6.0"]
                 [ring/ring-core "1.10.0"]
                 [metosin/malli "0.11.0"]
                 [metosin/muuntaja "0.6.8"]
                 [metosin/ring-swagger-ui "5.0.0-alpha.0"]
                 [crypto-password "0.3.0"]
                 [com.rpl/specter "1.1.4"]]
  :plugins [[lein-ring "0.12.6"]]
  :ring {:handler dop.core/app}
  :repl-options {:init-ns dop.core}
  :main dop.core)
