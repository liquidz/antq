(ns antq.format
  (:require
   [clojure.pprint :as pprint]))

(defn compare-deps
  [x y]
  (let [prj (.compareTo (:file x) (:file y))]
    (if (zero? prj)
      (.compareTo (:name x) (:name y))
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

(defn print-default-table
  [deps]
  (if (seq deps)
    (->> deps
         (sort compare-deps)
         skip-duplicated-file-name
         (map #(update % :latest-version (fnil identity "Failed to fetch")))
         (pprint/print-table [:file :name :version :latest-version]))
    (println "All dependencies are up-to-date.")))

(defn print-deps
  [deps options]
  (print-default-table deps))
