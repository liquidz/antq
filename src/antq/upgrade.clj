(ns antq.upgrade)

(defmulti upgrader
  (fn [version-checked-dep]
    (:project version-checked-dep)))

(defmethod upgrader :default
  [dep]
  (println
   (format "%s: Not supported yet."
           (name (:project dep)))))

(defn- confirm
  [dep]
  (print (format "Do you upgrade %s '%s' to '%s' in %s (y/n): "
                 (:name dep)
                 (:version dep)
                 (:latest-version dep)
                 (:file dep)))
  (flush)
  (contains? #{'y 'Y 'yes 'Yes 'YES} (read)))

(defn upgrade!
  [version-checked-deps interactive?]
  (doseq [dep version-checked-deps
          :when (if interactive?
                  (confirm dep)
                  true)]
    (when-let [upgraded-content (upgrader dep)]
      (println (format "Upgraded %s '%s' to '%s' in %s."
                       (:name dep)
                       (:version dep)
                       (:latest-version dep)
                       (:file dep)))
      (spit (:file dep) upgraded-content))))
