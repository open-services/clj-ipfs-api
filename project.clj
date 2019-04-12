(defproject ipfs-api "1.0.0"
  :description "Client IPFS API library with built-in daemon"
  :url "https://github.com/open-services/clj-ipfs-api"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :plugins [[jonase/eastwood "0.3.5"]]
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [clj-http "3.9.1"]
                 [cheshire "5.8.1"]]
  :repl-options {:init-ns ipfs-api.core})
