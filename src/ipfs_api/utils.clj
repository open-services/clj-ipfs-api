(ns ipfs-api.utils
  (:require [clj-http.client :as http]
            [clojure.string :refer [lower-case split join]]
            [clojure.walk :refer [postwalk]]))

(defn multiaddr->map [ma]
  (let [parts (clojure.string/split ma #"/")]
    {:host (get parts 2)
     :port (Integer/parseInt (get parts 4))}))

(comment
  (multiaddr->map "/ip4/127.0.0.1/tcp/5001")
  ;; => {:host "127.0.0.1" :port 5001}
  )

(defn format-http-addr [ma & parts]
  (let [api-addr (multiaddr->map ma)
        host (:host api-addr)
        port (:port api-addr)
        suffix (clojure.string/join "/" parts)]
    (format "http://%s:%s/api/v0/%s" host port suffix)))

(comment
  (format-http-addr "/ip4/127.0.0.1/tcp/5001" "version")
  ;; => http://127.0.0.1:5001/api/v0/version
  )

;;(keyword (clojure.string/lower-case (clojure.string/join
;; (rest (str :Version)))))

(defn lowercase-key [k]
  (-> k
      (str)
      (rest)
      (join)
      (lower-case)
      (keyword)))

(comment
  (lowercase-key :Version)
  ;; => :version
  )

(defn lowercase-keys
  "Recursively lower-case all map keys that are keywords"
  [m]
  (let [f (fn [[k v]] (if (keyword? k) [(lowercase-key k) v] [k v]))]
    (postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) m)))

(comment
  (lowercase-keys {:Version "1"})
  ;; => {:version "1"}
  (lowercase-keys {:My-Own-Version "1"})
  ;; => {:my-own-version "1"}
  (lowercase-keys {:MyOwnVersion "1"})
  ;; => {:myownversion "1"}
  )

(defn api-call
  ([conn endpoint]
   (lowercase-keys (api-call conn endpoint {:as :json})))
  ([conn endpoint opts]
   (api-call http/get conn endpoint opts))
 ([method conn endpoint opts]
   (let [addr (format-http-addr conn endpoint)]
     (:body (method addr opts)))))

(comment
  (api-call "/ip4/127.0.0.1/tcp/5001" "version")
  ;; => {:version "0.4.18"}
  )
