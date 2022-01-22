(ns antq.upgrade
  (:require
   [antq.download :as download]
   [antq.log :as log]))

(defmulti upgrader
  (fn [version-checked-dep]
    (:project version-checked-dep)))

(defmethod upgrader :default
  [dep]
  (log/error
   (format "%s: Not supported yet."
           (name (:project dep)))))

(defn- confirm
  [dep force?]
  (cond
    (and (:latest-version dep)
         force?)
    true

    ;; TODO: Remove this condition when upgrading YAML is supported
    (= :github-action (:project dep))
    false

    (:latest-version dep)
    (do (print (format "Do you want to upgrade %s '%s' to '%s' in %s (y/n): "
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
  [deps options]
  (let [force? (or (:force options) false)
        download? (or (:download options) false)
        version-checked-deps (filter :latest-version deps)]
    (when (and (seq version-checked-deps)
               (not force?))
      (log/info ""))

    (let [upgrade-result (group-by
                          (fn [dep]
                            (if (confirm dep force?)
                              (if-let [upgraded-content (upgrader dep)]
                                (do (log/info (format "Upgraded %s '%s' to '%s' in %s."
                                                      (:name dep)
                                                      (:version dep)
                                                      (:latest-version dep)
                                                      (:file dep)))
                                    (spit (:file dep) upgraded-content)
                                    true)
                                false)
                              false))
                          version-checked-deps)]
      (when download?
        (download/download! (get upgrade-result true)))
      (get upgrade-result false))))
