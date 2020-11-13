(ns antq.test-helper
  (:require
   [antq.record :as r]
   [clojure.data :as data]))

(defn test-dep
  [m]
  (r/map->Dependency (merge {:type :test} m)))

(defn- name-version-map
  [deps]
  (->> deps
       (map (juxt :name :version))
       (into {})))

(defn diff-deps
  [from-deps to-deps]
  (data/diff (name-version-map from-deps)
             (name-version-map to-deps)))
