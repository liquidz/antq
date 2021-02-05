(ns antq.log)

(defn info
  [s]
  (println s))

(defn error
  [s]
  (binding [*out* *err*]
    (println s)))
