(ns antq.util.async
  (:require
   [clojure.core.async :as async]))

(defn fn-with-timeout
  [f timeout-ms]
  (fn [& args]
    (let [ch (async/chan)]
      (async/go
        (let [ret (try
                    (apply f args)
                    (catch Throwable ex
                      [::exception ex]))]
          (async/>! ch ret)))
      (let [ret (-> [ch
                     (async/timeout timeout-ms)]
                    (async/alts!!)
                    (first))]

        (if (and (vector? ret) (= ::exception (first ret)))
          (throw (second ret))
          ret)))))
