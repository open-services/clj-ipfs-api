(ns ipfs-api.misc
  (:require [ipfs-api.utils :as utils]
            [clojure.edn :as edn]
            [clj-http.client :as http]
            [cheshire.core :refer [parse-string]])
  (:refer-clojure :exclude [cat]))

(defn version [conn]
  (utils/api-call conn "version"))

(defn directory? [object]
  (= (:data object) "\u0008\u0001"))

(defn index-file? [file]
  (= (:name file) "index.html"))

(defn find-first
  [f coll]
  (first (filter f coll)))

;; Part of IPFS Gateway
(defn get-index-file [object]
  (let [found-file (find-first index-file? (:links object))]
    (when-not (nil? found-file)
      (:hash found-file))))
(comment
  (cat "/ip4/127.0.0.1/tcp/5001" "QmcaeB6iZGzGjhcTEiq5DSpSmxaFNvyY8dda47RhHtBkaz")
  )


(defn object-get
  [conn multihash]
  (utils/api-call conn (str "object/get?arg=" multihash)))

(defn add-str [conn content]
  (let [multipart-v [{:name "none" :content content}]
        opts {:as :json :multipart multipart-v}]
      (:Hash (utils/api-call http/post conn "add" opts))))

(defn add-file [conn file]
  (let [multipart-v [{:name "none" :content file}]
        opts {:as :json :multipart multipart-v}]
      (:Hash (utils/api-call http/post conn "add" opts))))

(comment
  (add-file "/ip4/127.0.0.1/tcp/5001" (java.io.File. "test/fixtures/big.file"))
  (add-directory "/ip4/127.0.0.1/tcp/5001" "test/fixtures")
  )

(defn add-edn [conn structure]
  (add-str conn (prn-str structure)))

;; Helpers for `add-directory`
(defn starts-with-dot? "" [file] (= \. (first (.getName ^java.io.File file))))

(defn is-dir? "" [file] (.isDirectory ^java.io.File file))

(defn hide-dotfiles "" [files] (remove starts-with-dot? files))
(defn hide-dirs "" [files] (remove is-dir? files))

(defn hide-dotfiles-from-files-map [files]
  (remove #(= \. (first (:name %))) files))

(defn get-files "" [dir] (hide-dirs (file-seq (clojure.java.io/file dir))))

(comment
  (add-directory "/ip4/127.0.0.1/tcp/5001" "test/fixtures/dir")
  (add-directory "/ip4/127.0.0.1/tcp/5001" "test/fixtures/nested-dir")
  (clojure.pprint/pprint (get-files "test/fixtures/nested-dir"))
  (let [dir "test/fixtures/dir"
        multipart (files-to-multipart dir (get-files dir))]
    (add-multipart "/ip4/127.0.0.1/tcp/5001" multipart true))
  )

(def wrap-args "wrap-with-directory&progress=true&recursive=true&stream-channels=true")

(defn add-multipart
  ([conn multipart]
   (add-multipart conn multipart false))
  ([conn multipart wrap?]
   (if wrap?
     (utils/api-call http/post conn (str "add?" wrap-args) multipart)
     (utils/api-call http/post conn "add" multipart))))

(defn ndjson-str-to-map
  [string]
  (mapv parse-string (clojure.string/split-lines string)))

(defn get-hash-of-last
  [m]
  (get-in (last m) ["Hash"]))

(defn to-local-filename
  [dir file]
  (let [filename (.getPath ^java.io.File file)]
    (clojure.string/join "" (rest (clojure.string/replace filename dir "")))))

(comment
  (map #(to-local-filename "test/fixtures/nested-dir" %)
       (get-files "test/fixtures/nested-dir"))

  (clojure.string/join "" (rest "/dir-a/file-b"))
  )

(defn files-to-multipart
  [dir files]
  {:multipart
   (let [files (mapv #(into {} {:name (to-local-filename dir %)
                                :encoding "UTF-8"
                                :mime-type "file-body"
                                :content %}) files)]
     (hide-dotfiles-from-files-map files))})

(defn add-directory [conn directory]
  (let [files (get-files directory)]
    (get-hash-of-last
      (ndjson-str-to-map
          (add-multipart conn (files-to-multipart directory files) true)))))

(defn cat
  [conn multihash]
  (let [path (format
               "cat?arg=%s"
               multihash)]
    (utils/api-call conn path {})))

(defn cat->byte-array
  [conn multihash]
  (let [path (format
               "cat?arg=%s&encoding=json&stream-channels=true"
               multihash)]
    (utils/api-call conn path {:as :byte-array})))

(defn cat-edn
  [conn multihash]
  (edn/read-string (cat conn multihash)))

(comment
  (version "/ip4/127.0.0.1/tcp/5001")
  (add "/ip4/127.0.0.1/tcp/5001" "hello world")
  (cat "/ip4/127.0.0.1/tcp/5001" "QmWkekotdAk14KMSv3v9dpgQeb3cpDWVDVErpArT1jCbjK")
  (cat "/ip4/127.0.0.1/tcp/5001" "/ipfs/QmaunpKHv4s1HiJToYrUHRD77pB2gHrVmDPwqndYbwmgoe/fonts/Roboto-Black.ttf")
  )
