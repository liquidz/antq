(ns antq.report.edn
  (:require
   [antq.report :as report]))

(defmethod report/reporter "edn"
  [deps _options]
  (->> deps
       ;; Convert a record to just a map
       (map #(merge {} %))
       (pr-str)
       (println)))
