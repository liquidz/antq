(ns antq.log)

(def ^:dynamic *verbose* false)

(defn info
  [s]
  (println s))

(defn error
  [s]
  (binding [*out* *err*]
    (println s)))

(defn warning
  [s]
  (when *verbose*
    (binding [*out* *err*]
      (println s))))
