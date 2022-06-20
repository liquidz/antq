(ns antq.util.async
  (:require
   [clojure.core.async :as async]))

(defn fn-with-timeout
  [f timeout-ms]
  (fn [& args]
    (let [ch (async/chan)]
      (async/pipe (async/thread
                    (try
                      (apply f args)
                      (catch Throwable ex
                        ex)))
                  ch)
      (let [ret (-> [ch
                     (async/timeout timeout-ms)]
                    (async/alts!!)
                    (first))]
        (when (instance? Throwable ret)
          (throw ret))
        ret))))
