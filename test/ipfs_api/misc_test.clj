(ns ipfs-api.misc-test
  (:require [clojure.test :refer :all]
            [ipfs-api.test-utils :refer [with-daemon]]
            [ipfs-api.misc :as misc]
            [clojure.java.io :as io]))

(def expected-version
  {:version "0.5.0"
   :commit ""
   :repo "9"
   :system "amd64/linux"
   :golang "go1.13.10"})

(def test-string "hello world")
(def test-map {:name "Victor"})
(def test-list [{:a 1} {:b 2}])
(def test-dir-path "test/fixtures/dir")
(def test-dir-hash
  "Qmc8A6yG4FAKuKgGsr4tuNw897oFLHY8XQBv17L3VeHPb6")
(def test-html-dir-path "test/fixtures/dir-with-index-html")
(def test-html-dir-hash
  "QmcRyKPZGy1y6QQAkKNiNg45LZf51vaeJY3jD7XhtEX2z3")
(def test-html-dir-index-hash
  "QmcEfSsxkn6WCsJ6Ankkg2ioH7CyCAcBvpVYy8Eubo9SA8")
(def test-nested-dirs-path "test/fixtures/nested-dir")
(def test-nested-dirs-hash
  "QmUfT57ex2F6PH1ZyZGx678ykp2ujvKy1kdWCuYKFf8qAB")
(def test-big-file-path "test/fixtures/big.file")
(def test-big-file-hash "QmYgP636rx3FWH6R3Pe36nPLat3zS91bNNtzSNTSyvNJmK")
(def test-file-path "test/fixtures/dir/file-a.md")
(def test-file-hash "QmT4keV5qdMoYajXEerDuaJkxVhhKdFrKpBEsG7UzC81v8")

(deftest version
  (testing "Getting version from daemon"
    (with-daemon
      (is (= expected-version (misc/version daemon))))))

(deftest id
  (testing "Getting ID from daemon"
    (with-daemon
      (let [res (misc/id daemon)]
        (is (string? (:id res)))
        (is (string? (:publickey res)))
        (is (string? (:agentversion res)))
        (is (string? (:protocolversion res)))))))

(defn test-add [daemon content]
  (let [mh (misc/add-str daemon content)]
    (is (= (misc/cat daemon mh) content))))

(defn file->byte-array [path]
  (let [f (java.io.File. path)
        ary (byte-array (.length f))
        is (java.io.FileInputStream. f)]
    (.read is ary)
    (.close is)
    ary))

(deftest add
  (with-daemon
    (testing "add a string"
      (let [mh (misc/add-str daemon test-string)]
        (is (= (misc/cat daemon mh) test-string))))
    (testing "add a file"
      (let [mh (misc/add-file daemon (java.io.File. test-file-path))]
        (is (= mh test-file-hash))))
    (testing "add a map"
      (let [mh (misc/add-edn daemon test-map)]
        (is (= (misc/cat-edn daemon mh) test-map))))
    (testing "add a list"
      (let [mh (misc/add-edn daemon test-list)]
        (is (= (misc/cat-edn daemon mh) test-list))))
    (testing "add directory"
      (let [mh (misc/add-directory daemon test-dir-path)]
        (is (= mh test-dir-hash)))
      (let [mh (misc/add-directory daemon test-html-dir-path)]
        (is (= mh test-html-dir-hash))))
    (testing "add nested directory"
      (let [mh (misc/add-directory daemon test-nested-dirs-path)]
        (is (= mh test-nested-dirs-hash))))
    (testing "add a bigger file"
      (let [mh (misc/add-file daemon (java.io.File. test-big-file-path))]
        (is (= mh test-big-file-hash))))
    (testing "cat a bigger file"
      (let [content (misc/cat daemon test-big-file-hash)]
        (is (= (slurp test-big-file-path) content))))
    (testing "cat with byte-array"
      (let [content (misc/cat->byte-array daemon test-file-hash)
            expected (file->byte-array test-file-path)]
        (is (= (String. content) (String. expected)))
        (is (= (seq content) (seq expected)))))
    (testing "cat a bigger file with byte-array"
      (let [content (misc/cat->byte-array daemon test-big-file-hash)
            expected (file->byte-array test-big-file-path)]
        (is (= (String. content) (String. expected)))
        (is (= (seq content) (seq expected)))))
    (testing "gateway stuff"
      (let [mh (misc/add-directory daemon test-dir-path)]
        (is (= true (misc/directory? (misc/object-get daemon mh))))
        (is (= nil (misc/get-index-file (misc/object-get daemon mh)))))
      (let [mh (misc/add-directory daemon test-html-dir-path)]
        (is (= true (misc/directory? (misc/object-get daemon mh))))
        (is (= test-html-dir-index-hash (misc/get-index-file (misc/object-get daemon mh)))))
        )))
