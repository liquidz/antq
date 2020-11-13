(ns antq.upgrade.clojure
  (:require
   [antq.upgrade :as upgrade]
   [antq.util.dep :as u.dep]
   [antq.util.zip :as u.zip]
   [rewrite-cljc.zip :as z]))

(defn- in-deps?
  [loc]
  (->> loc z/up z/left z/value
       (contains? #{:deps :extra-deps})))

(defn upgrade-dep
  [loc version-checked-dep]
  (let [name-set (u.dep/name-candidates (:name version-checked-dep))
        version-key (if (= :git (:type version-checked-dep))
                      :sha
                      :mvn/version)]
    (loop [loc loc]
      (if-let [loc (z/find-value loc z/next name-set)]
        (recur (if (in-deps? loc)
                 (-> loc
                     ; move to map
                     (z/right) (z/down)
                     ; find target key
                     (z/find-value z/right version-key)
                     ; replace
                     (z/right)
                     (z/replace (:latest-version version-checked-dep)))
                 loc))
        (u.zip/move-to-root loc)))))

(defmethod upgrade/upgrader :clojure
  [version-checked-dep]
  (-> (z/of-file (:file version-checked-dep))
      (upgrade-dep version-checked-dep)
      (z/root-string)))
