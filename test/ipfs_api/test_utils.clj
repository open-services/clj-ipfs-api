(ns ipfs-api.test-utils
  (:require [ipfs-api.daemon :as daemon-ctl]))

;; these utils helps you to write tests! Isn't
;; that great

;; (defmacro like-run-with-daemon [f]
;;   (list
;;     ('println "doing something")
;;     ('let ['daemon true]
;;       (f))
;;     ('println "after doing something")))

;; CELEBRATE: this is my first macro!
(defmacro with-daemon [& f]
  (let [daemon 'daemon]
    `(let [~daemon (daemon-ctl/start)]
       ~@f
       (daemon-ctl/stop ~daemon))))

(comment
  (macroexpand '(with-daemon (println daemon)))
  (with-daemon
    (println daemon))
  (with-daemon
    (println "here is our daemon")
    (println daemon))
  )

;; (defn run-with-daemon [f]
;;   (daemon/start)
;;   (f)
;;   (daemon/stop))
