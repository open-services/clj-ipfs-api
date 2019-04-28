(ns ipfs-api.files
  (:require [ipfs-api.utils :as utils]
            [clj-http.client :as http]
            [clojure.edn :as edn])
  (:refer-clojure :exclude [read]))

(defn write [conn path content]
  (let [multipart-v [{:name "file"
                      :content content
                      :mime-type "application/octet-stream"}]
        path (format "files/write?arg=%s&stream-channels=true&create=true&p=true" path)
        opts {:as :json :multipart multipart-v}]
    (utils/api-call http/post conn path opts)))

(defn write-edn [conn path structure]
  (let [multipart-v [{:name path :content (prn-str structure)}]
        path (format "files/write?create=true&p=true&arg=%s" path)
        opts {:as :json :multipart multipart-v}]
    (utils/api-call http/post conn path opts)))

(defn path-exists? [conn path]
  (let [path (format "files/stat?arg=%s" path)]
    (try
      (do
        (utils/api-call http/get conn path {})
        true)
      (catch Exception _ false))))

(defn read
  ([conn path]
   (read conn path {}))
  ([conn path opts]
   (let [path (format "files/read?arg=%s" path)]
     (utils/api-call http/get conn path opts))))

(defn read-edn [conn path]
  (let [path (format "files/read?arg=%s" path)]
    (edn/read-string (utils/api-call http/get conn path {}))))

(defn rm [conn path]
  (let [path (format "files/rm?arg=%s" path)]
    (utils/api-call http/get conn path {})))

(defn stat [conn path]
  (let [path (format "files/stat?arg=%s" path)]
    (utils/api-call conn path)))

(comment
  (write "/ip4/127.0.0.1/tcp/5001" "/name" "victor")
  (read "/ip4/127.0.0.1/tcp/5001" "/name")
  (rm "/ip4/127.0.0.1/tcp/5001" "/name"))
