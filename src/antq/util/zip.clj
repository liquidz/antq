(ns antq.util.zip
  (:require
   [rewrite-clj.zip :as z]))

(defn move-to-root
  [loc]
  (loop [loc loc]
    (if-let [loc' (z/up loc)]
      (recur loc')
      loc)))
