(ns antq.test-helper
  (:require
   [antq.record :as r]
   [antq.upgrade.clojure]
   [lambdaisland.deep-diff2 :as ddiff])
  (:import
   (lambdaisland.deep_diff2.diff_impl
    Deletion
    Insertion
    Mismatch)))

(defn test-dep
  [m]
  (r/map->Dependency (merge {:type :test} m)))

(defn name-version-sorted-list
  [deps]
  (->> deps
       (map #(let [url (get-in % [:extra :url])
                   sha (get-in % [:extra :sha])]
               (cond-> (select-keys % [:name :version])
                 url (assoc :url url)
                 sha (assoc :sha sha))))
       (sort-by #(str (:name %) (:version %)))))

(defn diff-deps
  [expected-deps actual-deps]
  (->> (ddiff/diff (name-version-sorted-list expected-deps)
                   (name-version-sorted-list actual-deps))
       (filter #(or (instance? Insertion %)
                    (instance? Deletion %)
                    (instance? Mismatch (:version %))
                    (instance? Mismatch (:name %))))
       ;; convert `:version`, `:sha` and `:name` to a simple map
       (map #(cond-> %
               (and (contains? % :version)
                    (map? (:version %)))
               (update :version (fn [m] (merge {} m)))

               (and (contains? % :sha)
                    (map? (:sha %)))
               (update :sha (fn [m] (merge {} m)))

               (and (contains? % :name)
                    (map? (:name %)))
               (update :name (fn [m] (merge {} m)))

               (or (contains? % :+)
                   (contains? % :-))
               (->> (merge {}))))
       (set)))

(defn diff-lines
  [expected-lines actual-lines]
  (->> (ddiff/diff expected-lines
                   actual-lines)
       (filter #(instance? Mismatch %))
       (map #(select-keys % [:- :+]))
       (set)))
