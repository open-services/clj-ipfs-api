(ns ipfs-api.files-test
  (:require [clojure.test :refer :all]
            [ipfs-api.test-utils :refer [with-daemon]]
            [ipfs-api.daemon :as daemon]
            [ipfs-api.files :as files]
            [clojure.java.io :as io]))

;; (deftest version
;;   (testing "Getting version from daemon"
;;     (with-daemon
;;       (is (= expected-version (misc/version daemon))))))

(def test-string "hello world")
(def test-file-path "/my-file")
(def test-nested-path "/my-directory/1/2/3/4/5/my-file")
(def test-another-nested-path "/npmjs.org/a-package/tarball.tgz")
(def test-map {:name "Victor"})
(def test-list [{:a 1} {:b 2}])
(def test-small-file-path "test/fixtures/dir/file-a.md")
(def test-big-file-path "test/fixtures/big.file")

(defn file->byte-array [path]
  (let [f (java.io.File. path)
        ary (byte-array (.length f))
        is (java.io.FileInputStream. f)]
    (.read is ary)
    (.close is)
    ary))

(defn test-create-file [daemon path content]
  ;; Create new file
  (files/write daemon path content)
  ;; Read created file
  (is (= (files/read daemon path) content))
  ;; Remove the file
  (files/rm daemon path)
  ;; File doesn't exists anymore
  (is (thrown? Exception (files/read daemon path))))

(defn test-create-edn [daemon path structure]
  ;; Create new file
  (files/write-edn daemon path structure)
  ;; Read created file
  (is (= (files/read-edn daemon path) structure))
  ;; Remove the file
  (files/rm daemon path)
  ;; File doesn't exists anymore
  (is (thrown? Exception (files/read-edn daemon path))))

(deftest files-create
  (with-daemon
    (testing "create file"
      (test-create-file daemon test-file-path test-string))
    (testing "create nested file"
      (test-create-file daemon test-nested-path test-string)
      (test-create-file daemon test-another-nested-path test-string))
    (testing "create file with clojure data in it"
      (test-create-edn daemon test-nested-path test-map)
      (test-create-edn daemon test-nested-path test-list))
    (testing "create file with a InputStream"
      (let [stream (io/input-stream (.getBytes "hello world"))]
        (files/write daemon "/hello" stream)
        (is (= (files/read daemon "/hello") "hello world"))
        (files/rm daemon "/hello")))
    (testing "create file with byte-array"
      (let [b (.getBytes "hello world")]
        (files/write daemon "/hello" b)
        (is (= (files/read daemon "/hello") "hello world"))
        (files/rm daemon "/hello")))
    (testing "create from file with byte-array"
      (let [b (file->byte-array test-small-file-path)]
        (files/write daemon "/hello-lol/world" b)
        (is (= (count (files/read daemon "/hello-lol/world")) (count b)))
        (is (= (files/read daemon "/hello-lol/world") (String. b)))
        (files/rm daemon "/hello-lol/world")))))
;; (testing "create big file with byte-array"
;;   (let [b (file->byte-array test-big-file-path)]
;;     (files/write daemon "/hello" b)
;;     (is (= (count (files/read daemon "/hello")) (count b)))
;;     (files/rm daemon "/hello")))
;; (testing "create big file with byte-array"
;;   (let [b (file->byte-array test-big-file-path)]
;;     (files/write daemon "/dir-lol/world" b)
;;     (is (= (count (files/read daemon "/dir-lol/world")) (count b)))
;;     (is (= b (files/read daemon "/dir-lol/world")))
;;     (files/rm daemon "/dir-lol/world")))

(deftest files-read
  (with-daemon
    (testing "read a file as byte-array"
      (let [b (file->byte-array test-small-file-path)]
        (files/write daemon "/hello-lol/world" b)
        (is (= (count (files/read daemon "/hello-lol/world")) (count b)))
        (is (= (String. (files/read daemon "/hello-lol/world" {:as :byte-array}))
               (String. b)))
        (files/rm daemon "/hello-lol/world")))))

(deftest files-create
  (with-daemon
    (testing "stat a directory"
      (let [_ (files/write daemon "/hello-lol/world" (file->byte-array test-small-file-path))
            res (files/stat daemon "/hello-lol")]
        (is (= (:hash res) "Qmcf3v3DpHUBTM5bEQ46bqbCrNKKnZmGc7WbGYwGgT3a8p"))
        (is (= (:size res) 0))
        (is (= (:cumulativesize res) 120))
        (is (= (:blocks res) 1))
        (is (= (:type res) "directory"))))))

(deftest files-ls
  (with-daemon
    (testing "ls a directory"
      (let [b (file->byte-array test-small-file-path)]
        (files/write daemon "/hello-lol/hello" b)
        (files/write daemon "/hello-lol/world" b)
        (let [res (files/ls daemon "/hello-lol")]
          (is (= (count res) 2))
          (is (= res [{:name "hello", :type 0, :size 0, :hash ""}
                      {:name "world", :type 0, :size 0, :hash ""}])))
        (let [res (files/ls daemon "/")]
          (is (= (count res) 1))
          (is (= res [{:name "hello-lol", :type 0, :size 0, :hash ""}])))
        (files/rm daemon "/hello-lol/hello")
        (files/rm daemon "/hello-lol/world")))))
