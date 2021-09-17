(ns antq.upgrade.clojure
  (:require
   [antq.upgrade :as upgrade]
   [antq.util.dep :as u.dep]
   [antq.util.zip :as u.zip]
   [rewrite-clj.zip :as z]))

(defn- in-deps?
  [loc]
  (->> loc z/up z/left z/sexpr
       (contains? #{:deps :extra-deps :replace-deps :override-deps})))

(defn- find-version-key
  [loc version-checked-dep]
  (if (= :git-sha (:type version-checked-dep))
    (or (z/find-value loc z/right :sha)
        (z/find-value loc z/right :git/sha))
    (z/find-value loc z/right :mvn/version)))

(defn upgrade-dep
  [loc version-checked-dep]
  (let [name-set (u.dep/name-candidates (:name version-checked-dep))]
    (loop [loc loc]
      (if-let [loc (z/find-value loc z/next name-set)]
        (recur (if (in-deps? loc)
                 (some-> loc
                         ;; move to map
                         (z/right) (z/down)
                         ;; find version key
                         (find-version-key version-checked-dep)
                         ;; replace
                         (z/right)
                         (z/replace (:latest-version version-checked-dep)))
                 (z/next loc)))
        (u.zip/move-to-root loc)))))

(defmethod upgrade/upgrader :clojure
  [version-checked-dep]
  (-> (z/of-file (:file version-checked-dep))
      (upgrade-dep version-checked-dep)
      (z/root-string)))
