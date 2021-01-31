(ns antq.upgrade.boot
  (:require
   [antq.upgrade :as upgrade]
   [antq.util.dep :as u.dep]
   [antq.util.zip :as u.zip]))

(require (if u.zip/rewrite-cljc-supported?
           '[rewrite-cljc.zip :as z]
           '[antq.stub.rewrite-cljc.zip :as z]))

(defn- in-dependencies?
  [loc]
  (-> loc z/up z/up z/up z/left z/value
      (= :dependencies)))

(defn upgrade-dep
  [loc version-checked-dep]
  (let [name-set (u.dep/name-candidates (:name version-checked-dep))]
    (loop [loc loc]
      (if-let [loc (z/find-value loc z/next name-set)]
        (recur (if (in-dependencies? loc)
                 (-> loc z/right (z/replace (:latest-version version-checked-dep)))
                 (z/next loc)))
        (u.zip/move-to-root loc)))))


(defmethod upgrade/upgrader :boot
  [version-checked-dep]
  (-> (z/of-file (:file version-checked-dep))
      (upgrade-dep version-checked-dep)
      (z/root-string)))
