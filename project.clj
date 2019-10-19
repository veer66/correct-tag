(defproject correct-tags "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.10.0"]]
  :main ^:skip-aot correct-tags.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
