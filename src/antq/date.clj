(ns antq.date)

(defmulti get-last-updated-at
  (fn [dep _options]
    (:type dep)))

(defmethod get-last-updated-at :default
  [_ _]
  nil)
