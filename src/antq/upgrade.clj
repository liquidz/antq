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
  [dep force?]
  (cond
    (and (:latest-version dep)
         force?)
    true

    (:latest-version dep)
    (do (print (format "Do you upgrade %s '%s' to '%s' in %s (y/n): "
                       (:name dep)
                       (:version dep)
                       (:latest-version dep)
                       (:file dep)))
        (flush)
        (contains? #{'y 'Y 'yes 'Yes 'YES} (read)))

    :else
    false))

(defn upgrade!
  "Return only non-upgraded deps"
  [version-checked-deps force?]
  (doall
   (remove
    (fn [dep]
      (if (confirm dep force?)
        (if-let [upgraded-content (upgrader dep)]
          (do (println (format "Upgraded %s '%s' to '%s' in %s."
                               (:name dep)
                               (:version dep)
                               (:latest-version dep)
                               (:file dep)))
              (spit (:file dep) upgraded-content)
              true)
          false)
        false))
    version-checked-deps)))
