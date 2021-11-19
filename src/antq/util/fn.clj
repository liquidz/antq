(ns antq.util.fn)

(defn memoize-by
  [f key-fn]
  (let [mem (atom {})]
    (fn [m & args]
      (if-let [res (get @mem (get m key-fn))]
        res
        (let [ret (apply f m args)]
          (swap! mem assoc (get m key-fn) ret)
          ret)))))

(comment
  ;; for testing
  (defn memoize-by [f _key-fn] f))
