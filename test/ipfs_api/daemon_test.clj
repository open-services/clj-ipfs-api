(ns ipfs-api.daemon-test
  (:require [clojure.test :refer :all]
            [ipfs-api.daemon :as daemon]
            [ipfs-api.misc :as misc]))

(deftest daemon-control
  (testing "starts daemon"
    (let [conn (daemon/start)]
      (is (not (nil? conn)))
      (is (= "0.4.20" (:version (misc/version conn))))
      (is (= true (daemon/running? conn)))
      (daemon/stop conn))))
