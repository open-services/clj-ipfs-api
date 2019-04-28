(ns ipfs-api.daemon
  (:require [clojure.java.shell :refer [sh]]
            [ipfs-api.utils :as utils]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [me.raynes.fs.compression :as compression]
            [me.raynes.fs :as fs]))

(def home (System/getProperty "user.home"))

(def api-file-path (format "%s/%s" home ".ipfs/api"))

(def default-version "v0.4.20")
(def default-platform "linux-amd64")
(def root-hash "QmPPPSBDbmJJYAHYpHpZQcLRiBavgxGbkzfKVBuFmZcE6N")
(def gateway "https://ipfs.io/ipfs/%s")

(defn get-binary-path [version platform]
  (format "/go-ipfs/%s/go-ipfs_%s_%s.tar.gz" version version platform))

(comment
  (get-binary-path "v0.4.20" "linux-amd64")
  ;; => /go-ipfs/v0.4.20/go-ipfs_v0.4.20_linux-amd64.tar.gz
  )

(defn get-download-url [root-hash gateway binary-path]
  (str (format gateway root-hash) binary-path))

(defn copy [uri file]
  (with-open [in (io/input-stream uri)
              out (io/output-stream file)]
    (io/copy in out)))

(defn file-exists? [p]
  (.exists (io/as-file p)))

(defn download-binary [version platform root-hash]
  (let [p "./go-ipfs.tar.gz"]
    (when-not (file-exists? p)
      (let [binary-path (get-binary-path version platform)
            tar-path "./binaries/go-ipfs.tar.gz"]
      (copy (get-download-url root-hash gateway binary-path) tar-path)))))

(defn get-final-binary-path [version platform]
  (format "./binaries/go-ipfs_%s_%s" version platform))

(defn extract-binary [version platform start-tar end-dir]
  (compression/gunzip "./binaries/go-ipfs.tar.gz" "./binaries/go-ipfs.tar")
  (compression/untar "./binaries/go-ipfs.tar" "./binaries")
  (fs/chmod "+x" "./binaries/go-ipfs/ipfs")
  (fs/rename "./binaries/go-ipfs/ipfs" (get-final-binary-path version platform)))

(defn clean-install []
  (fs/delete-dir "./binaries/go-ipfs")
  (fs/delete "./binaries/go-ipfs.tar")
  (fs/delete "./binaries/go-ipfs.tar.gz"))

(comment
  (download-binary "linux-amd64" root-hash)
  (extract-binary "./binaries/go-ipfs.tar.gz" "./binaries")
  (clean-install))

(defn install [version platform]
  (when-not (file-exists? (get-final-binary-path version platform))
    (println (format "Downloading binary for version %s platform %s" version platform))
    (download-binary version platform root-hash)
    (println "Extracting")
    (extract-binary version platform "./binaries/go-ipfs.tar.gz" "./binaries")
    (println "Cleaning")
    (clean-install)))

(defn running? [conn]
  (try (utils/api-call "/ip4/127.0.0.1/tcp/5001" "version")
       true
       (catch Exception e
         false)))

(defn wait-until-running [conn]
  (let [alive? (atom false)]
    (while (not @alive?)
      (reset! alive? (running? conn))
      (Thread/sleep 100))))

(defn format-shell-error [ret]
  (let [code (:exit ret)
        err (:err ret)]
    (format "Got error: '%s' and return code '%s'" code err)))

(def errors (atom []))

;; versions = v0.4.20
;; platform = darwin | freebsd | linux | windows

(defn format-daemon-cmd [version platform]
  (let [binary-path (get-final-binary-path version platform)]
    (format "IPFS_PATH=$(mktemp -d) %s daemon --offline --init" binary-path)))

(defn start
  ([] (start default-version default-platform))
  ([version platform]
   (install version platform)
   (let [conn "/ip4/127.0.0.1/tcp/5001"]
     (future
       (let [ret (sh "bash" "-c" (format-daemon-cmd version platform))]
         (when (not= 0 (:exit ret))
           (println (str "error: " (:exit ret)))
           (pprint ret)
           (swap! errors conj ret)
           (System/exit (:exit ret)))))
     (wait-until-running conn)
     conn)))

(defn stop [conn]
  (utils/api-call conn "shutdown")
  (doseq [error @errors]
    (pprint error))
    ;; (println (:exit error))
    ;; (println (:err error)))
  )
