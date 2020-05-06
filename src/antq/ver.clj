(ns antq.ver)

(defmulti get-latest-version :type)

(defmethod get-latest-version :default
  [dep]
  (throw (ex-info "Unknown dependency type" dep)))
