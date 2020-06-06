(ns antq.format
  (:require
   [clojure.pprint :as pprint]
   [clojure.string :as str]))

(def ^:private default-outdated-message-format
  "{{name}} {{version}} is outdated. Latest version is {{latest-version}}.")

(def ^:private default-failed-message-format
  "Failed to fetch the latest version of {{name}} {{version}}.")

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

(defn apply-format-string
  [dep format-string]
  (reduce-kv (fn [s k v]
               (str/replace s (str "{{" (name k) "}}") (or v "")))
             format-string
             (select-keys dep [:file :name :version :latest-version :message])))

(defn print-by-error-format
  [deps format-string]
  (when (seq deps)
    (doseq [s (->> deps
                   (sort compare-deps)
                   (map #(assoc % :message
                                (if (:latest-version %)
                                  (apply-format-string % default-outdated-message-format)
                                  (apply-format-string % default-failed-message-format))))
                   (map #(apply-format-string % format-string)))]
      (println s))))

(defn print-deps
  [deps options]
  (if-let [fmt (:error-format options)]
    (print-by-error-format deps fmt)
    (print-default-table deps)))
