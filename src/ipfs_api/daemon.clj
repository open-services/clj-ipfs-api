(ns ipfs-api.daemon
  (:require [clojure.java.shell :refer [sh]]
            [ipfs-api.utils :as utils]))

(def home (System/getProperty "user.home"))

(def api-file-path (format "%s/%s" home ".ipfs/api"))

;; Old version for running that assumed the 
;; api file from go-ipfs was created after the
;; daemon was actually listening on the sockets
;; (defn running? []
;;   (try (slurp api-file-path)
;;        true
;;        (catch Exception e
;;               false)))

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

(defn start []
  "Starts a go-ipfs daemon"
  (let [conn "/ip4/127.0.0.1/tcp/5001"]
    (future
      (let [ret (sh "bash" "-c" "IPFS_PATH=$(mktemp -d) binaries/ipfs.linux.x86-64 daemon --offline --init")]
        (when (not= 0 (:exit ret))
          (println (str "error: " (:exit ret)))
          (clojure.pprint/pprint ret)
          (swap! errors conj ret)
          (throw (Exception. (format-shell-error ret))))))
    (wait-until-running conn)
    conn))

(defn stop [conn]
  "Shutdowns the go-ipfs daemon cleanly"
  (utils/api-call conn "shutdown")
  (doseq [error @errors]
    (clojure.pprint/pprint error))
    ;; (println (:exit error))
    ;; (println (:err error)))
  )
