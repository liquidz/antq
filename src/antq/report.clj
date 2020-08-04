(ns antq.report)

(defmulti reporter
  (fn [_deps options]
    (:reporter options)))

(defmethod reporter :default
  [_ options]
  (println "Unknown reporter:" (:reporter options)))
