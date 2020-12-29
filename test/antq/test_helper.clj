(ns antq.test-helper
  (:require
   [antq.record :as r]
   [antq.test-helper :as h]
   [antq.upgrade.clojure]
   [lambdaisland.deep-diff2 :as ddiff])
  (:import
   lambdaisland.deep_diff2.diff_impl.Mismatch))

(defn test-dep
  [m]
  (r/map->Dependency (merge {:type :test} m)))

(defn name-version-sorted-list
  [deps]
  (->> deps
       (map #(select-keys % [:name :version]))
       (sort-by #(str (:name %) (:version %)))))

(defn diff-deps
  [expected-deps actual-deps]
  (->> (ddiff/diff (name-version-sorted-list expected-deps)
                   (name-version-sorted-list actual-deps))
       (filter #(instance? Mismatch (:version %)))
       ;; convert `:version` to a simple map
       (map #(update % :version (fn [m] (merge {} m))))
       (set)))
