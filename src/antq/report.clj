(ns antq.report
  (:require
   [antq.log :as log]))

(defmulti reporter
  (fn [_deps options]
    (:reporter options)))

(defmethod reporter :default
  [_ options]
  (log/error (str "Unknown reporter: " (:reporter options))))
