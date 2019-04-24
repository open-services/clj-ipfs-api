(ns ipfs-api.files-check
  (:require
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]))


;; ipfs mfs api
;; chcid [<path>]
;; cp <source> <dest>
;; flush [<path>]
;; ls [<path>]
;; mkdir <path>
;; mv <source> <dest>
;; read <path>
;; rm <path>
;; stat <path>
;; write <path> <data>

;; not exposed
;; flush
;; chcid

;; additional exposed api
;; exists?
;; clear! => removes "/"

(defn rand-bool [] (> 0.5 (rand)))
(comment
  (rand-bool))

(defn s-prefix [a] (str "/" a))

(defn str-add-rand [to-add s]
  (clojure.string/join "" (assoc (clojure.string/split s #"") (+ (int (rand (count s))) 1) to-add)))

(defn maybe-do [s f]
  (if (rand-bool) (f s) s))

(defn random-path [a] (s-prefix (maybe-do a (partial str-add-rand "/"))))

(defn valid-path [a] (let [s (set a)]
                       (if (= (count s) 1)
                         false
                         (not (= (last (clojure.string/split a #"")) "/")))))

(def gen-path (gen/such-that valid-path
                             (gen/fmap random-path
                                       gen/string-alphanumeric)))

(def gen-content gen/string)
(def gen-write (gen/return [:write gen-path gen-content]))

;; (deftest files-contains

(comment
  (clojure.pprint/pprint (gen/sample gen-path 100))
  (clojure.pprint/pprint (gen/sample gen-content 100))
  )

(defn db-create [] (atom {}))
(defn db-write [db path content] (swap! db assoc path content))
(defn db-read [db path] (get @db path))
(defn db-clear [db] (reset! db {}))
(defn db-delete [db path] (swap! db dissoc path))
(defn db-size [db] (count @db))

(comment
  (db-read (db-write (db-create) "/hello" "world") "/hello")
  (let [p "/hello"
        c "das"
        db (db-create)]
    (db-write db p c)
    (= (db-read db p) c))
  )

(defspec mfs-contains 1000
  (prop/for-all [p gen-path
                 c gen-content]
                (let [db (db-create)]
                  (db-write db p c)
                  (= c (db-read db p)))))

(defspec mfs-overwrite 1000
  (prop/for-all [p gen-path
                 c gen-content
                 c2 gen-content]
                (let [db (db-create)]
                  (db-write db p c)
                  (db-write db p c2)
                  (= c2 (db-read db p)))))

(defspec mfs-clear 1000
  (prop/for-all [p gen-path
                 c gen-content]
                (let [db (db-create)]
                  (db-write db p c)
                  (db-clear db)
                  (zero? (db-size db)))))

(comment
  (clojure.pprint/pprint (mfs-contains))
  (clojure.pprint/pprint (mfs-overwrite))
  (clojure.pprint/pprint (mfs-clear))
  )

(def gen-clear (gen/return [:clear]))
(def gen-size (gen/return [:size]))
(def gen-write (gen/tuple (gen/return :write)
                          gen-path
                          gen-content))

(def gen-delete (gen/tuple (gen/return :delete)
                           gen-path))

(def gen-read (gen/tuple (gen/return :read)
                         gen-path))

(def gen-ops (gen/vector (gen/one-of [gen-clear
                                      gen-write
                                      gen-delete
                                      gen-read
                                      gen-size])))

(comment
  (gen/sample gen-ops)
  )

(defn db-run [db ops]
  (doseq [[op k v] ops]
    (case op
      :clear (db-clear db)
      :size (db-size db)
      :write (db-write db k v)
      :delete (db-delete db k)
      :read (db-read db k)
      )))

(defn hm-run [db ops]
  (reduce
    (fn [hm [op k v]]
      (case op
        :clear {}
        :size hm
        :write (assoc hm k v)
        :delete (dissoc hm k)
        :read hm))
    db ops))

(defn equiv? [db hm]
  (and (= (count hm) (db-size db))
       (every? (fn [[k v]]
                 (= v (db-read db k)))
               hm)))

(defspec hash-map-equiv 1000
  m
  (prop/for-all [ops gen-ops]
                (let [hm (hm-run {} ops)
                      db (db-create)]
                  (db-run db ops)
                  (equiv? db hm))))

(comment
  (clojure.pprint/pprint (hash-map-equiv))
  )

;; generate sequence of actions to take
;; throw in some threads
;; check consistency
