(ns antq.report.table
  (:require
   [antq.log :as log]
   [antq.report :as report]
   [antq.util.dep :as u.dep]
   [antq.util.file :as u.file]
   [antq.util.ver :as u.ver]
   [clojure.pprint :as pprint]
   [clojure.set :as set]))

(def ^:private progress (atom nil))

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
  ;; Show table
  (if (seq deps)
    (->> deps
         (sort u.dep/compare-deps)
         skip-duplicated-file-name
         (map #(assoc % :latest-version (u.ver/normalize-latest-version %)))
         (map #(update % :file u.file/normalize-path))
         (map #(let [latest-key (if (seq (:latest-name %))
                                  :latest-name
                                  :latest-version)]
                 (set/rename-keys % {:version :current
                                     latest-key :latest})))
         (pprint/print-table [:file :name :current :latest]))
    (println "All dependencies are up-to-date."))

  ;; Show changes URLs
  (let [urls (->> deps
                  (filter :latest-version)
                  (sort u.dep/compare-deps)
                  (keep :changes-url)
                  (distinct))]
    (when (seq urls)
      (println "\nAvailable changes:")
      (doseq [u urls]
        (println "-" u)))))

(defn- progress-text
  [{:keys [width total-count current-count]}]
  (let [width (or width 50)
        ratio (int (* width (/ current-count total-count)))]
    (format "[%s%s] %d/%d\r"
            (apply str (repeat ratio "#"))
            (apply str (repeat (- width ratio) "-"))
            current-count
            total-count)))

(defmethod report/init-progress "table"
  [deps _options]
  (reset! progress {:total-count (count deps)
                    :count-atom (atom 0)}))

(defmethod report/run-progress "table"
  [_dep _options]
  (when-let [{:keys [total-count count-atom]} @progress]
    (when (< @count-atom total-count)
      (swap! count-atom inc)
      (log/async-print
       (progress-text {:total-count total-count :current-count @count-atom})))))

(defmethod report/deinit-progress "table"
  [_ _]
  (println ""))
