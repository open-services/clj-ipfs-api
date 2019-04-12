(ns ipfs-api.utils-test
  (:require [clojure.test :refer :all]
            [ipfs-api.utils :as utils]))

(deftest multiaddr->map
  (testing "turn multiaddr into map"
    (let [m (utils/multiaddr->map "/ip4/127.0.0.1/tcp/5001")]
      (is (= {:host "127.0.0.1" :port 5001} m)))))

(deftest format-http-addr
  (testing "turns multiaddr + parts into http URI"
    (let [uri (utils/format-http-addr "/ip4/127.0.0.1/tcp/5001" "version")]
      (is (= "http://127.0.0.1:5001/api/v0/version" uri)))))

(deftest lowercase-key
  (testing "turns keyword with any case to lowercase"
    (is (= :version (utils/lowercase-key :Version)))
    (is (= :version (utils/lowercase-key :VerSion)))))

(deftest lowercase-keys
  (testing "turns map with keywords to lowercase"
    (is (= {:version 1} (utils/lowercase-keys {:Version 1})))))
