(ns antq.util.zip)

(let [{:keys [major minor]} *clojure-version*]
  (def rewrite-cljc-supported?
    (or (and (= major 1) (>= minor 9))
        (> major 1))))

(require (if rewrite-cljc-supported?
           '[rewrite-cljc.zip :as z]
           '[antq.stub.rewrite-cljc.zip :as z]))

(defn move-to-root
  [loc]
  (loop [loc loc]
    (if-let [loc' (z/up loc)]
      (recur loc')
      loc)))
