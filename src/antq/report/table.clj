(ns antq.report.table
  (:require
   [antq.report :as report]
   [antq.util.dep :as u.dep]
   [antq.util.ver :as u.ver]
   [clojure.pprint :as pprint]))

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
         (map #(assoc % :latest-version (u.ver/normalize-latest-version %)))
         (pprint/print-table [:file :name :version :latest-version]))
    (println "All dependencies are up-to-date.")))
