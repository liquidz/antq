(ns antq.util.zip)

(let [{:keys [major minor]} *clojure-version*]
  (def rewrite-clj-supported?
    (or (and (= major 1) (>= minor 9))
        (> major 1))))

(require (if rewrite-clj-supported?
           '[rewrite-clj.zip :as z]
           '[antq.stub.rewrite-clj.zip :as z]))

(defn move-to-root
  [loc]
  (loop [loc loc]
    (if-let [loc' (z/up loc)]
      (recur loc')
      loc)))
