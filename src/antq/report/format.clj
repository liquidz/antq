(ns antq.report.format
  (:require
   [antq.report :as report]
   [antq.util.dep :as u.dep]
   [antq.util.ver :as u.ver]
   [clojure.string :as str]))

(def ^:private default-outdated-message-format
  "{{name}} {{version}} is outdated. Latest version is {{latest-version}}. {{diff-url}}")

(def ^:private default-failed-message-format
  "Failed to fetch the latest version of {{name}} {{version}}.")

(defn apply-format-string
  [dep format-string]
  (let [dep (-> dep
                (assoc :latest-version (u.ver/normalize-latest-version dep))
                (select-keys [:file :name :version :latest-version :message :diff-url]))]
    (reduce-kv (fn [s k v]
                 (str/replace s (str "{{" (name k) "}}") (or v "")))
               format-string
               dep)))

(defmethod report/reporter "format"
  [deps options]
  (let [format-string (:error-format options)]
    (when (seq deps)
      (doseq [s (->> deps
                     (sort u.dep/compare-deps)
                     (map #(assoc % :message
                                  (if (:latest-version %)
                                    (apply-format-string % default-outdated-message-format)
                                    (apply-format-string % default-failed-message-format))))
                     (map #(apply-format-string % format-string)))]
        (println s)))))
