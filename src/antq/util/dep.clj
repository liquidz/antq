(ns antq.util.dep)

(defn compare-deps
  [x y]
  (let [prj (.compareTo ^String (:file x) ^String (:file y))]
    (if (zero? prj)
      (.compareTo ^String (:name x) ^String (:name y))
      prj)))
