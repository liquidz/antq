(ns antq.report.table
  (:require
   [antq.report :as report]
   [antq.util.dep :as u.dep]
   [clojure.pprint :as pprint]))

(defn compare-deps
  [x y]
  (let [prj (.compareTo ^String (:file x) ^String (:file y))]
    (if (zero? prj)
      (.compareTo ^String (:name x) ^String (:name y))
      prj)))

(defn skip-duplicated-file-name
  [sorted-deps]
  (loop [[dep & rest-deps] sorted-deps
         last-file nil
         result []]
    (if-not dep
      result
      (if (= last-file (:file dep))
        (recur rest-deps last-file (conj result (assoc dep :file "")))
        (recur rest-deps (:file dep) (conj result dep))))))

(defmethod report/reporter "table"
  [deps _options]
  (if (seq deps)
    (->> deps
         (sort u.dep/compare-deps)
         skip-duplicated-file-name
         (map #(update % :latest-version (fnil identity "Failed to fetch")))
         (pprint/print-table [:file :name :version :latest-version]))
    (println "All dependencies are up-to-date.")))
